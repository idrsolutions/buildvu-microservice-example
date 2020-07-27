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
package com.idrsolutions.microservice;

import com.idrsolutions.microservice.utils.SettingsValidator;
import com.idrsolutions.microservice.utils.ZipHelper;
import org.jpedal.examples.BuildVuConverter;
import org.jpedal.render.output.IDRViewerOptions;

import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import java.io.File;
import java.io.IOException;
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

    private static final Logger LOG = Logger.getLogger(BuildVuServlet.class.getName());

    private static final String[] validTextModeOptions = {
            "svg_realtext",
            "svg_shapetext_selectable",
            "svg_shapetext_nonselectable",
            "image_realtext",
            "image_shapetext_selectable",
            "image_shapetext_nonselectable"};

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

            final BuildVuConverter converter = new BuildVuConverter(inFile, outputDir, paramMap, new IDRViewerOptions());
            converter.convert();

            ZipHelper.zipFolder(outputDirStr + "/" + fileNameWithoutExt,
                                outputDirStr + "/" + fileNameWithoutExt + ".zip");

            final String outputPathInDocroot = individual.getUuid() + "/" + fileNameWithoutExt;

            individual.setValue("previewUrl", contextUrl + "/output/" + outputPathInDocroot + "/index.html");
            individual.setValue("downloadUrl", contextUrl + "/output/" + outputPathInDocroot + ".zip");

            individual.setState("processed");

        } catch (final Exception ex) {
            LOG.log(Level.SEVERE, "Exception thrown when trying to convert file", ex);
            individual.doError(1220, "error occurred whilst converting the file");
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
                individual.doError(1050, "Libreoffice timed out after 1 minute"); // Libreoffice killed after 1 minute
                break;
            case 2:
                individual.doError(1070, "Internal error processing file"); // Internal error
                break;
            default:
                individual.doError(1100, "An internal error has occurred"); // Internal error
                break;
        }
    }

    @Override
    protected SettingsValidator validateSettings(final String settings) {
        final String[] conversionParams = settings != null ? getConversionParams(settings) : null;

        final SettingsValidator settingsValidator = new SettingsValidator(conversionParams);

        settingsValidator.validateString("org.jpedal.pdf2html.textMode", validTextModeOptions, false);
        settingsValidator.validateBoolean("org.jpedal.pdf2html.compressSVG", false);
        settingsValidator.validateBoolean("org.jpedal.pdf2html.embedImagesAsBase64Stream", false);
        settingsValidator.validateBoolean("org.jpedal.pdf2html.convertSpacesToNbsp", false);
        settingsValidator.validateBoolean("org.jpedal.pdf2html.convertPDFExternalFileToOutputType", false);
        settingsValidator.validateBoolean("org.jpedal.pdf2html.keepGlyfsSeparate", false);
        settingsValidator.validateBoolean("org.jpedal.pdf2html.separateTextToWords", false);
        settingsValidator.validateBoolean("org.jpedal.pdf2html.compressImages", false);
        settingsValidator.validateBoolean("org.jpedal.pdf2html.useLegacyImageFileType", false);
        settingsValidator.validateFloat("org.jpedal.pdf2html.imageScale", new float[]{1, 10}, false);
        settingsValidator.validateString("org.jpedal.pdf2html.includedFonts", new String[]{"woff", "otf", "woff_base64", "otf_base64"}, false);
        settingsValidator.validateBoolean("org.jpedal.pdf2html.disableComments", false);
        settingsValidator.validateString("org.jpedal.pdf2html.realPageRange", "(\\s*((\\d+\\s*-\\s*\\d+)|(\\d+\\s*:\\s*\\d+)|(\\d+))\\s*(,|$)\\s*)+", false);
        settingsValidator.validateString("org.jpedal.pdf2html.logicalPageRange", "(\\s*((\\d+\\s*-\\s*\\d+)|(\\d+\\s*:\\s*\\d+)|(\\d+))\\s*(,|$)\\s*)+", false);
        settingsValidator.validateString("org.jpedal.pdf2html.scaling", "(\\d+\\.\\d+)|(\\d+x\\d+)|(fitWidth\\d+)|(fitHeight\\d+)|(\\d+)", false);
        settingsValidator.validateString("org.jpedal.pdf2html.viewMode", new String[]{"content"}, false);
        settingsValidator.validateBoolean("org.jpedal.pdf2html.completeDocument", false);
        settingsValidator.validateString("org.jpedal.pdf2html.viewerUI", new String[]{"complete", "clean", "simple", "slideshow", "custom"}, false);
        settingsValidator.validateString("org.jpedal.pdf2html.containerId", ".*", false);
        settingsValidator.validateBoolean("org.jpedal.pdf2html.generateSearchFile", false);
        settingsValidator.validateBoolean("org.jpedal.pdf2html.outputThumbnails", false);


        return settingsValidator;
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
            e.printStackTrace(); // soffice location may need to be added to the path
            LOG.severe(e.getMessage());
            return 2;
        }
        return 0;
    }
}
