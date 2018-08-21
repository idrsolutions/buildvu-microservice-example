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

@WebServlet(name = "buildvu", urlPatterns = {"/buildvu"})
@MultipartConfig
public class BuildVuServlet extends BaseServlet {

    private static final Logger LOG = Logger.getLogger(BuildVuServlet.class.getName());

    @Override
    void convert(QueueItem data) {

        final String[] settings = data.params.get("settings");
        final String[] conversionParams = settings != null ? getConversionParams(settings[0]) : null;
        final String fileName = data.inputFile.getName();
        final String ext = fileName.substring(fileName.lastIndexOf(".") + 1);
        final String fileNameWithoutExt = fileName.substring(0, fileName.lastIndexOf("."));
        // To avoid repeated calls to getParent() and getAbsolutePath()
        final String inputDir = data.inputFile.getParent();
        final String outputDir = data.outputDir.getAbsolutePath();
        
        final String userPdfFilePath;

        final boolean isPDF = ext.toLowerCase().endsWith("pdf");
        if (!isPDF) {
            final int result = convertToPDF(data.inputFile);
            if (result != 0) {
                data.individual.state = "error";
                setErrorCode(data.individual, result);
                return;
            }
            userPdfFilePath = inputDir + "/" + fileNameWithoutExt + ".pdf";
        } else {
            userPdfFilePath = inputDir + "/" + fileName;
        }

        //Makes the directory for the output file
        new File(outputDir + "/" + fileNameWithoutExt).mkdirs();

        data.individual.state = "processing";

        try {

            data.individual.outputDir = outputDir + "/" + fileNameWithoutExt;

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
            final PDFtoHTML5Converter html = new PDFtoHTML5Converter(inFile, data.outputDir, options, new IDRViewerOptions());
            html.convert();

            ZipHelper.zipFolder(outputDir + "/" + fileNameWithoutExt,
                                outputDir + "/" + fileNameWithoutExt + ".zip");

            final String outputPathInDocroot = data.individual.uuid + "/" + fileNameWithoutExt;

            data.individual.setValue("previewUrl", data.contextUrl + "/output/" + outputPathInDocroot + "/index.html");
            data.individual.setValue("downloadUrl", data.contextUrl + "/output/" + outputPathInDocroot + ".zip");

            data.individual.state = "processed";

        } catch (final Exception ex) {
            ex.printStackTrace();
            LOG.severe(ex.getMessage());
            data.individual.state = "error";
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
