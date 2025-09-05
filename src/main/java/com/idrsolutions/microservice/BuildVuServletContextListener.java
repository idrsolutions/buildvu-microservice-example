/*
 * BuildVu Microservice Example
 *
 * Project Info: https://github.com/idrsolutions/buildvu-microservice-example
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

import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This servlet context listener is responsible for initializing the BuildVu microservice.
 * It sets up the base path for the output files and validates the configuration file values.
 */
@WebListener
public class BuildVuServletContextListener extends BaseServletContextListener {

    /** The configuration property key used to specify whether to include the generated PDF in the output when processing office documents. */
    public static final String KEY_PROPERTY_INCLUDE_OFFICE_PDF = "includeOfficePdf";

    /** Logger instance used for logging messages within this class. */
    private static final Logger LOG = Logger.getLogger(BuildVuServletContextListener.class.getName());

    /**
     * Retrieves the configuration path used by the application, which is "~/.idr/buildvu-microservice/".
     *
     * @return the configuration path as a String.
     */
    @Override
    public String getConfigPath() {
        String userDir = System.getProperty("user.home");
        if (!userDir.endsWith("/") && !userDir.endsWith("\\")) {
            userDir += System.getProperty("file.separator");
        }
        return userDir + "/.idr/buildvu-microservice/";
    }

    /**
     * Retrieves the name of the configuration file used by the application.
     *
     * @return the name of the configuration file, which is "buildvu-microservice.properties"
     */
    @Override
    public String getConfigName(){
        return "buildvu-microservice.properties";
    }

    /**
     * Initializes the servlet context when the application starts.
     *
     * @param servletContextEvent the event containing the servlet context that is being initialized
     */
    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
        super.contextInitialized(servletContextEvent);
        final Properties propertiesFile = (Properties) servletContextEvent.getServletContext().getAttribute(KEY_PROPERTIES);
        OutputFileServlet.setBasePath(propertiesFile.getProperty(KEY_PROPERTY_OUTPUT_PATH));

        if (DBHandler.isUsingMemoryDatabase()) {
            final String message = "It is recommended to set your own database instead of using the default internal database as it will allow you to more easily scale the service in the future.\n" +
                    "More details on the benefits and how to do this can be found here https://support.idrsolutions.com/buildvu/tutorials/cloud/options/external-state-database";
            LOG.log(Level.WARNING, message);
        }
    }

    /**
     * Validates the configuration values provided in the Properties object by invoking specific validation methods for
     * individual properties. Ensures that the required configuration properties have appropriate values and applies
     * default values or logs warnings if necessary.
     *
     * @param propertiesFile the Properties object containing the configuration values to be validated
     */
    @Override
    protected void validateConfigFileValues(final Properties propertiesFile) {
        super.validateConfigFileValues(propertiesFile);

        validateLibreOfficePath(propertiesFile);
        validateLibreOfficeTimeout(propertiesFile);
        validateIncludeOfficePdf(propertiesFile);
    }

    /**
     * Validates the "libreOfficePath" property in the provided Properties object.
     * If the property is not set or contains an empty value, it assigns a default value of "soffice" and logs a
     * warning.
     *
     * @param properties the Properties object containing configuration properties to be validated
     */
    private static void validateLibreOfficePath(final Properties properties) {
        final String libreOfficePath = properties.getProperty(KEY_PROPERTY_LIBRE_OFFICE);
        if (libreOfficePath == null || libreOfficePath.isEmpty()) {
            properties.setProperty(KEY_PROPERTY_LIBRE_OFFICE, "soffice");
            LOG.log(Level.WARNING, "Properties value for \"libreOfficePath\" was not set. Using a value of \"soffice\"");
        }
    }

    /**
     * Validates the "libreOfficeTimeout" property in the provided Properties object.
     * If the property is not set, is empty, or contains an invalid value (non-numeric),
     * it assigns a default value of "60000" and logs a warning.
     *
     * @param properties the Properties object containing configuration properties to be validated
     */
    private static void validateLibreOfficeTimeout(final Properties properties) {
        final String libreOfficeTimeout = properties.getProperty(KEY_PROPERTY_LIBRE_OFFICE_TIMEOUT);
        if (libreOfficeTimeout == null || libreOfficeTimeout.isEmpty() || !libreOfficeTimeout.matches("\\d+")) {
            properties.setProperty(KEY_PROPERTY_LIBRE_OFFICE_TIMEOUT, "60000");
            LOG.log(Level.WARNING, "Properties value for \"libreOfficeTimeout\" was not set. Using a value of \"60000\"");
        }
    }

    /**
     * Validates the "includeOfficePdf" property in the provided Properties object.
     * If the property is not set or is invalid, it assigns a default value of "false" and logs a warning.
     *
     * @param properties the Properties object containing configuration properties to be validated
     */
    private static void validateIncludeOfficePdf(final Properties properties) {
        final String includeOfficePdf = properties.getProperty(KEY_PROPERTY_INCLUDE_OFFICE_PDF);
        if (includeOfficePdf == null || includeOfficePdf.isEmpty() || !Boolean.parseBoolean(includeOfficePdf)) {
            properties.setProperty(KEY_PROPERTY_INCLUDE_OFFICE_PDF, "false");
            if (!"false".equalsIgnoreCase(includeOfficePdf)) {
                final String message = String.format("Properties value for \"includeOfficePdf\" was set to \"%s\" " +
                        "but should be a boolean. Using a value of false.", includeOfficePdf);
                LOG.log(Level.WARNING, message);
            }
        }
    }
}
