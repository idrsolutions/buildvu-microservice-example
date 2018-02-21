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

@WebServlet(name = "buildvu", urlPatterns = {"/buildvu"})
@MultipartConfig
public class BuildVuServlet extends BaseServlet {

    protected void convert(final Individual individual, final Map<String, String[]> parameterMap, final String fileName,
                           final String inputDirectory, final String outputDirectory,
                           final String fileNameWithoutExt, final String ext) {

        final String[] settings = parameterMap.get("settings");
        final String[] conversionParams = settings != null ? getConversionParams(settings[0]) : null;

        final String userPdfFilePath;

        final boolean isPDF = ext.toLowerCase().endsWith("pdf");
        if (!isPDF) {
            final int result = convertToPDF(fileName, inputDirectory);
            if (result != 0) {
                individual.state = "error";
                setErrorCode(individual, result);
                return;
            }
            userPdfFilePath = inputDirectory + "/" + fileNameWithoutExt + ".pdf";
        } else {
            userPdfFilePath = inputDirectory + "/" + fileName;
        }

        //Makes the directory for the output file
        new File(outputDirectory + "/" + fileNameWithoutExt).mkdirs();

        individual.state = "processing";

        try {

            individual.outputDir = outputDirectory + "/" + fileNameWithoutExt;

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
            final File outDir = new File(outputDirectory + "/");

            final HTMLConversionOptions options = new HTMLConversionOptions(paramMap);
            final PDFtoHTML5Converter html = new PDFtoHTML5Converter(inFile, outDir, options, new IDRViewerOptions());
            html.convert();


            ZipHelper.zipFolder(outputDirectory + "/" + fileNameWithoutExt, outputDirectory + "/" + fileNameWithoutExt + ".zip");

            final String outputDir = individual.uuid + "/" + fileNameWithoutExt;

            individual.setValue("previewPath", "output/" + outputDir + "/index.html");
            individual.setValue("downloadPath", "output/" + outputDir + "/index.html");

            individual.state = "processed";

        } catch (final Exception ex) {
            ex.printStackTrace();
            individual.state = "error";
        }
    }

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
     * Converts an office file to PDF
     * @param fileName Name of the office file to convert
     * @param directory Directory where the office file exists
     * @return 0 if success, 1 if libreoffice timed out, 2 if process error occurs
     */
    private static int convertToPDF(final String fileName, final String directory) {
        final ProcessBuilder pb = new ProcessBuilder("soffice", "--headless", "--convert-to", "pdf", fileName);
        pb.directory(new File(directory));
        final Process process;

        try {
            process = pb.start();
            if (!process.waitFor(1, TimeUnit.MINUTES)) {
                process.destroy();
                return 1;
            }
        } catch (final IOException | InterruptedException e) {
            e.printStackTrace(); // soffice location may need to be added to the path
            return 2;
        }
        return 0;
    }

    @Override
    void updateProgress(final Individual individual) {
        //
    }
}
