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
import org.jpedal.examples.html.PDFtoHTML5Converter;
import org.jpedal.render.output.IDRViewerOptions;
import org.jpedal.render.output.html.HTMLConversionOptions;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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

    private static final Logger LOG = Logger.getLogger(BuildVuServlet.class.getName());

    /**
     * Converts given pdf file or office document to html using BuildVu or
     * LibreOffice respectively.
     * <p>
     * See API docs for information on how this method communicates via the
     * individual object to the client.
     *
     * @param individual The individual object associated with this conversion
     * @param parameterMap The map of parameters that came with the request
     * @param fileName the file name of the input file
     * @param inputDirectory the input directory of the file
     * @param outputDirectory the output directory of the converted html
     * @param fileNameWithoutExt the input filename without its file extension
     * @param ext the file exension of the input file
     * @param contextURL The context that this servlet is running in
     */
    @Override
    void convert(Individual individual, Map<String, String[]> params,
            File inputFile, File outputDir, String contextUrl) {

        final String[] settings = params.get("settings");
        final String[] conversionParams = settings != null ? getConversionParams(settings[0]) : null;
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
                individual.state = "error";
                setErrorCode(individual, result);
                return;
            }
            userPdfFilePath = inputDir + "/" + fileNameWithoutExt + ".pdf";
        } else {
            userPdfFilePath = inputDir + "/" + fileName;
        }

        //Makes the directory for the output file
        new File(outputDirStr + "/" + fileNameWithoutExt).mkdirs();

        individual.state = "processing";

        try {

            individual.outputDir = outputDirStr + "/" + fileNameWithoutExt;

            final HashMap<String, String> paramMap = new HashMap<>();
            if (conversionParams != null) { //handle string based parameters
                if (conversionParams.length % 2 == 0) {
                    for (int z = 0; z < conversionParams.length; z = z + 2) {
                        paramMap.put(conversionParams[z], conversionParams[z + 1]);
                    }
                } else {
                    throw new Exception("Invalid length of String arguments");
                }
            }

            final File inFile = new File(userPdfFilePath);

            final HTMLConversionOptions options = new HTMLConversionOptions(paramMap);
            final PDFtoHTML5Converter html = new PDFtoHTML5Converter(inFile, outputDir, options, new IDRViewerOptions());
            html.convert();

            ZipHelper.zipFolder(outputDirStr + "/" + fileNameWithoutExt,
                                outputDirStr + "/" + fileNameWithoutExt + ".zip");

            final String outputPathInDocroot = individual.uuid + "/" + fileNameWithoutExt;

            individual.setValue("previewUrl", contextUrl + "/output/" + outputPathInDocroot + "/index.html");
            individual.setValue("downloadUrl", contextUrl + "/output/" + outputPathInDocroot + ".zip");

            individual.state = "processed";

        } catch (final Exception ex) {
            ex.printStackTrace();
            LOG.severe(ex.getMessage());
            individual.state = "error";
        }
    }

    /**
     * Set the error code in the given individual object. Error codes are based
     * on the return values of 
     * {@link BuildVuServlet#convertToPDF(String, String)}
     *
     * @param individual the individual object associated with this conversion
     * @param errorCode The return code to be parsed to an error code
     */
    private void setErrorCode(final Individual individual, final int errorCode) {
        switch (errorCode) {
            case 1:
                individual.errorCode = String.valueOf(1050); // Libreoffice killed after 1 minute
                break;
            case 2:
                individual.errorCode = String.valueOf(1070); // Internal error
                break;
            default:
                individual.errorCode = String.valueOf(1100); // Internal error
                break;
        }
    }

    /**
     * Converts an office file to PDF.
     *
     * @param fileName Name of the office file to convert
     * @param directory Directory where the office file exists
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
            e.printStackTrace(); // soffice location may need to be added to the path
            LOG.severe(e.getMessage());
            return 2;
        }
        return 0;
    }
}
