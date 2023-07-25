/*
 * BuildVu Microservice Example
 *
 * Project Info: https://github.com/idrsolutions/buildvu-microservice-example
 *
 * Copyright 2022 IDRsolutions
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

import com.idrsolutions.microservice.db.DBHandler;
import com.idrsolutions.microservice.storage.Storage;
import com.idrsolutions.microservice.utils.ConversionTracker;
import com.idrsolutions.microservice.utils.DefaultFileServlet;
import com.idrsolutions.microservice.utils.LibreOfficeHelper;
import com.idrsolutions.microservice.utils.ZipHelper;
import org.jpedal.PdfDecoderServer;
import org.jpedal.examples.BuildVuConverter;
import org.jpedal.exception.PdfException;
import org.jpedal.render.output.ContentOptions;
import org.jpedal.render.output.IDRViewerOptions;
import org.jpedal.render.output.OutputModeOptions;
import org.jpedal.settings.BuildVuSettingsValidator;

import javax.json.stream.JsonParsingException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides an API to use BuildVu on its own dedicated app server. See the API
 * documentation for more information on how to interact with this servlet.
 *
 * @see BaseServlet
 */
@WebServlet(name = "buildvu", urlPatterns = "/buildvu")
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
     * @param uuid The uuid of the conversion
     * @param inputFile The input file
     * @param outputDir The output directory of the converted file
     * @param contextUrl The context that this servlet is running in
     */
    @Override
    protected void convert(String uuid,
                           File inputFile, File outputDir, String contextUrl) {

        final Map<String, String> conversionParams;
        try {
            final Map<String, String> settings = DBHandler.getInstance().getSettings(uuid);
            conversionParams = settings != null ? settings : new HashMap<>();
        } catch (final SQLException e) {
            DBHandler.getInstance().setError(uuid, 500, "Database failure");
            return;
        }

        final String fileName = inputFile.getName();
        final String ext = fileName.substring(fileName.lastIndexOf(".") + 1);
        // To avoid repeated calls to getParent() and getAbsolutePath()
        final String inputDir = inputFile.getParent();
        final String outputDirStr = outputDir.getAbsolutePath();
        final Properties properties = (Properties) getServletContext().getAttribute(BaseServletContextListener.KEY_PROPERTIES);

        final File inputPdf;
        final boolean isPDF = ext.toLowerCase().endsWith("pdf");
        if (!isPDF) {
            final String libreOfficePath = properties.getProperty(BaseServletContextListener.KEY_PROPERTY_LIBRE_OFFICE);
            final long libreOfficeTimeout = Long.parseLong(properties.getProperty(BaseServletContextListener.KEY_PROPERTY_LIBRE_OFFICE_TIMEOUT));
            LibreOfficeHelper.Result libreOfficeConversionResult = LibreOfficeHelper.convertDocToPDF(libreOfficePath, inputFile, uuid, libreOfficeTimeout);
            switch (libreOfficeConversionResult) {
                case TIMEOUT:
                    DBHandler.getInstance().setError(uuid, libreOfficeConversionResult.getCode(), "Maximum conversion duration exceeded.");
                    return;
                case ERROR:
                    DBHandler.getInstance().setError(uuid, libreOfficeConversionResult.getCode(), "Internal error processing file");
                    return;
                case SUCCESS:
                    inputPdf = new File(inputDir, uuid + ".pdf");
                    if (!inputPdf.exists()) {
                        LOG.log(Level.SEVERE, "LibreOffice error found while converting to PDF: " + inputPdf.getAbsolutePath());
                        DBHandler.getInstance().setError(uuid, 1080, "Error processing PDF");
                        return;
                    }
                default:
                    LOG.log(Level.SEVERE, "Unexpected error has occurred converting office document: " + libreOfficeConversionResult.getCode() + " using LibreOffice");
                    DBHandler.getInstance().setError(uuid, libreOfficeConversionResult.getCode(), "Failed to convert office document to PDF");
                    return;
            }
        } else {
            inputPdf = new File(inputDir, fileName);
        }

        //Makes the directory for the output file
        final File conversionDir = new File(outputDirStr, uuid);
        conversionDir.mkdirs();
        final int pageCount;
        try {
            final PdfDecoderServer decoder = new PdfDecoderServer(false);
            decoder.openPdfFile(inputPdf.getAbsolutePath());

            decoder.setEncryptionPassword(conversionParams.getOrDefault("org.jpedal.pdf2html.password", ""));

            if (decoder.isEncrypted() && !decoder.isPasswordSupplied()) {
                LOG.log(Level.SEVERE, "Invalid Password");
                DBHandler.getInstance().setError(uuid, 1070, "Invalid password supplied.");
                return;
            }

            pageCount = decoder.getPageCount();
            DBHandler.getInstance().setCustomValue(uuid, "pageCount", String.valueOf(pageCount));
            DBHandler.getInstance().setCustomValue(uuid, "pagesConverted", "0");
            decoder.closePdfFile();
            decoder.dispose();
        } catch (final PdfException e) {
            LOG.log(Level.SEVERE, "Invalid PDF", e);
            DBHandler.getInstance().setError(uuid, 1060, "Invalid PDF");
            return;
        }

        DBHandler.getInstance().setState(uuid, "processing");

        try {
            final boolean isContentMode = "content".equalsIgnoreCase(conversionParams.remove("org.jpedal.pdf2html.viewMode"));

            final OutputModeOptions outputModeOptions = isContentMode ? new ContentOptions(conversionParams) : new IDRViewerOptions(conversionParams);

            final String originalFileName = DBHandler.getInstance().getCustomData(uuid).get("originalFileName");
            conversionParams.put("org.jpedal.pdf2html.originalFileName", originalFileName);
            conversionParams.put("org.jpedal.pdf2html.omitNameDir", "true");

            final BuildVuConverter converter = new BuildVuConverter(inputPdf, conversionDir, conversionParams, outputModeOptions);

            final long maxDuration = Long.parseLong(properties.getProperty(BaseServletContextListener.KEY_PROPERTY_MAX_CONVERSION_DURATION));
            converter.setCustomErrorTracker(new ConversionTracker(uuid, maxDuration));

            converter.convert();

            if ("1230".equals(DBHandler.getInstance().getStatus(uuid).get("errorCode"))) {
                final String message = String.format("Conversion %s exceeded max duration of %dms", uuid, maxDuration);
                LOG.log(Level.INFO, message);
                return;
            }

            ZipHelper.zipFolder(outputDirStr + "/" + uuid,
                    outputDirStr + "/" + uuid + ".zip");

            if (!isContentMode) {
                DBHandler.getInstance().setCustomValue(uuid, "previewUrl", contextUrl + "/output/" + uuid + "/index.html");
            }
            DBHandler.getInstance().setCustomValue(uuid, "downloadUrl", contextUrl + "/output/" + uuid + ".zip");

            final Storage storage = (Storage) getServletContext().getAttribute("storage");

            if (storage != null) {
                final String remoteUrl = storage.put(new File(outputDirStr + "/" + uuid + ".zip"), uuid + ".zip", uuid);
                DBHandler.getInstance().setCustomValue(uuid, "remoteUrl", remoteUrl);
            }

            DBHandler.getInstance().setState(uuid, "processed");
        } catch (final Throwable ex) {
            LOG.log(Level.SEVERE, "Exception thrown when converting input", ex);
            DBHandler.getInstance().setError(uuid, 1220, "Exception thrown when converting input" + ex.getMessage());
        }
    }

    /**
     * Validates the settings parameter passed to the request. It will parse the conversionParams,
     * validate them, and then set the params in the Individual object.
     *
     * If settings are not parsed or validated, doError will be called.
     *
     * @param request the request for this conversion
     * @param response the response object for the request
     * @param uuid the uuid of this conversion
     * @return true if the settings are parsed and validated successfully, false if not
     */
    @Override
    protected boolean validateRequest(final HttpServletRequest request, final HttpServletResponse response,
                                      final String uuid) {

        final Map<String, String> settings;
        try {
            settings = parseSettings(request.getParameter("settings"));
        } catch (JsonParsingException exception) {
            doError(request, response, "Error encountered when parsing settings JSON <" + exception.getMessage() + '>', 400);
            return false;
        }

        try {
            BuildVuSettingsValidator.validate(settings, false);
        } catch(final IllegalArgumentException e) {
            doError(request, response, e.getMessage(), 400);
            return false;
        }

        request.setAttribute("com.idrsolutions.microservice.settings", settings);

        return true;
    }
}
