/*
 * BuildVu Microservice Example
 *
 * Project Info: https://github.com/idrsolutions/buildvu-microservice-example
 *
 * Copyright 2018 IDRsolutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package conversion;

import conversion.utils.ZipHelper;
import conversion.utils.GCPHelper;
import org.jpedal.examples.BuildVuConverter;
import org.jpedal.render.output.IDRViewerOptions;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides an API to use BuildVu on its own dedicated app server. See the API
 * documentation for more information on how to interact with this servlet.
 * 
 * @see BaseServlet
 */
@WebServlet(name = "buildvu", urlPatterns = {"/buildvu"})
@MultipartConfig
public class BuildVuServlet extends BaseServlet {
    
    static {
        final String defaultOutput = System.getProperty("com.idrsolutions.defaultOutput");
        DEFAULT_OUTPUT = defaultOutput != null ? defaultOutput.toLowerCase() : "local";
        
        final boolean useTempDir = Boolean.getBoolean("com.idrsolutions.useTempDir");
        if (useTempDir) {
            BuildVuServlet.useTempDir();
        }
    }

    private static final Logger LOG = Logger.getLogger(BuildVuServlet.class.getName());
    private static final String DEFAULT_OUTPUT;

    /**
     * Converts given pdf file or office document to html or svg using BuildVu-HTML
     * and BuildVu-SVG respectively.
     * <p>
     * LibreOffice is used to preconvert office documents to PDF for BuildVu to
     * process.
     * <p>
     * See API docs for information on how this method communicates via the
     * individual object to the client.
     * 
     * @param individual The individual object associated with this conversion
     * @param params The map of parameters that came with the request
     * @param inputFile The input file
     * @param outputDir The output directory of the converted file
     * @param contextUrl The context that this servlet is running in
     */
    @Override
    protected void convert(Individual individual, Map<String, String[]> params,
                           File inputFile, File outputDir, String contextUrl) {

        final String[] settings = params.get("settings");
        final String[] conversionParams = settings != null ? getConversionParams(settings[0]) : null;
        final String[] rawOutput = params.get("output");
        final String output = rawOutput != null ? rawOutput[0].toLowerCase() : DEFAULT_OUTPUT;
        final String[] rawOutputOptions = params.get("outputOptions");
        final String[] outputOptions = rawOutputOptions != null ? getConversionParams(rawOutputOptions[0]) : null;
        final String fileName = inputFile.getName();
        final String ext = fileName.substring(fileName.lastIndexOf(".") + 1);
        final String fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf("."));
        // To avoid repeated calls to getParent() and getAbsolutePath()
        final String inputDir = inputFile.getParent();
        final String outputDirStr = outputDir.getAbsolutePath();
        
        final String userPdfFilePath;

        final boolean isPDF = ext.toLowerCase().endsWith("pdf");
        if (!isPDF) {
            final int result = convertToPDF(inputFile);
            if (result != 0) {
                individual.setState("error");
                setErrorCode(individual, result);
                return;
            }
            userPdfFilePath = inputDir + "/" + fileNameWithoutExt + ".pdf";
        } else {
            userPdfFilePath = inputDir + "/" + fileName;
        }

        //Makes the directory for the output file
        new File(outputDirStr + "/" + fileNameWithoutExt).mkdirs();

        individual.setState("processing");

        try {
            final Map<String, String> paramMap = strArrToMap(conversionParams);
            final File inFile = new File(userPdfFilePath);

            final BuildVuConverter converter = new BuildVuConverter(inFile, outputDir, paramMap, new IDRViewerOptions());
            converter.convert();

            ZipHelper.zipFolder(outputDirStr + "/" + fileNameWithoutExt,
                                outputDirStr + "/" + fileNameWithoutExt + ".zip");
            
            switch (output) {
                case "gcp":
                    individual.setState("starting output upload");
                    GCPHelper.handleGCPUpload(individual, outputDirStr + "/" + fileNameWithoutExt + ".zip",
                                              "application/zip", strArrToMap(outputOptions));
                    individual.setState("processed");
                    deleteLocalFiles(inputDir, outputDirStr);
                    break;
                case "local":
                    final String outputPathInDocroot = individual.getUuid() + "/" + fileNameWithoutExt;

                    individual.setValue("previewUrl", contextUrl + "/output/" + outputPathInDocroot + "/index.html");
                    individual.setValue("downloadUrl", contextUrl + "/output/" + outputPathInDocroot + ".zip");
                    individual.setState("processed");
                    break;
                default:
                    setErrorCode(individual, 3);
                    break;
            }
            
        } catch (final Exception ex) {
            LOG.log(Level.SEVERE, "Exception thrown when converting file", ex);
            setErrorCode(individual, 0);
        }
    }

    /**
     * Set the error code in the given individual object. Error codes are based
     * on the return values of 
     * {@link BuildVuServlet#convertToPDF(File)}
     *
     * @param individual the individual object associated with this conversion
     * @param errorCode The return code to be parsed to an error code
     */
    private void setErrorCode(final Individual individual, final int errorCode) {
        switch (errorCode) {
            case 1:
                individual.doError(1050); // Libreoffice killed after 1 minute
                break;
            case 2:
                individual.doError(1070); // Internal error
                break;
            case 3:
                individual.doError(1080); // Invalid Output Method
                break;
            default:
                individual.doError(1110); // Internal error
                break;
        }
    }
    
    /**
     * Converts a String array in the form [key1, val1, etc...] into a 
     * Map<String, String>.
     * 
     * @param strArr a String array in the form [key1, val1, key2, val2, etc...]
     * @return a Map<String, String> with the key/val from the input string 
     * array
     * @throws Exception when an invalid strArr is provided
     */
    private Map<String, String> strArrToMap(final String[] strArr) throws Exception {
        final Map<String, String> map = new HashMap<>();
        if (strArr != null) { //handle string based parameters
            if (strArr.length % 2 == 0) {
                for (int z = 0; z < strArr.length; z = z + 2) {
                    map.put(strArr[z], strArr[z + 1]);
                }
            } else {
                throw new Exception("Invalid length of String arguments");
            }
        }
        return map;
    }
    
    /**
     * Deletes the local files in both the input directory and output directory. 
     * The deletion is recursive so all files inside will be deleted.
     * 
     * @param inputDir The path to the local input directory
     * @param outputDir The path to the local output directory
     * @throws IOException When an issue is encountered when deleting the file
     */
    private void deleteLocalFiles(final String inputDir, final String outputDir) throws IOException {
        final File inputFile = new File(inputDir);
        final Path inputPath = inputFile.toPath();
        Files.walk(inputPath).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        
        if (inputFile.exists()) {
            LOG.log(Level.SEVERE, "Local input directory was not deleted successfully");
        }
        
        final File outputFile = new File(outputDir);
        final Path outputPath = outputFile.toPath();
        Files.walk(outputPath).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        
        if (outputFile.exists()) {
            LOG.log(Level.SEVERE, "Local output directory was not deleted successfully");
        }
    }

    /**
     * Converts an office file to PDF using LibreOffice.
     *
     * @param file The office file to convert to PDF
     * @return 0 if success, 1 if libreoffice timed out, 2 if process error
     * occurs
     */
    private static int convertToPDF(final File file) {
        final ProcessBuilder pb = new ProcessBuilder("soffice", "--headless", "--convert-to", "pdf", file.getName());
        pb.directory(new File(file.getParent()));
        final Process process;

        try {
            process = pb.start();
            if (!process.waitFor(1, TimeUnit.MINUTES)) {
                process.destroy();
                return 1;
            }
        } catch (final IOException | InterruptedException e) {
            LOG.log(Level.SEVERE, "Exception thrown in LibreOffice conversion", e); // soffice location may need to be added to the path
            return 2;
        }
        return 0;
    }
    
    /**
     * Makes use of the java.io.tmpdir property to create and then use docroot 
     * input/output folders to store files for conversions locally. If tmpdir is
     * unset, the directory will not be created or used.
     */
    private static void useTempDir() {
        final String tmpdir = System.getProperty("java.io.tmpdir");
        if (tmpdir != null) {
            final String inputStr = tmpdir + "/docroot/input/";
            final String outputStr = tmpdir + "/docroot/output/";
            final File inputPath = new File(inputStr);
            final File outputPath = new File(outputStr);
            
            try {
                if (!inputPath.exists()) {
                    inputPath.mkdirs();
                }
                if (!outputPath.exists()) {
                    outputPath.mkdirs();
                }
            } catch (SecurityException se) {
                LOG.log(Level.SEVERE, "SecurityException thrown when creating docroot directories", se);
            }
            
            BaseServlet.setInputPath(inputStr);
            BaseServlet.setOutputPath(outputStr);
        } else {
            LOG.log(Level.SEVERE, "java.io.tmpdir is unset, docroot has not been set up in the temp directory");
        }
    }
}