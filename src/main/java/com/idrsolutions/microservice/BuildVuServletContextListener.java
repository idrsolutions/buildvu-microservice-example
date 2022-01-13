package com.idrsolutions.microservice;

import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebListener
public class BuildVuServletContextListener extends BaseServletContextListener {

    private static final Logger LOG = Logger.getLogger(BuildVuServletContextListener.class.getName());

    @Override
    public String getConfigPath() {
        String userDir = System.getProperty("user.home");
        if (!userDir.endsWith("/") && !userDir.endsWith("\\")) {
            userDir += System.getProperty("file.separator");
        }
        return userDir + "/.idr/buildvu-microservice/";
    }

    @Override
    public String getConfigName(){
        return "buildvu-microservice.properties";
    }

    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
        super.contextInitialized(servletContextEvent);
        final Properties propertiesFile = (Properties) servletContextEvent.getServletContext().getAttribute(KEY_PROPERTIES);
        OutputFileServlet.setBasePath(propertiesFile.getProperty(KEY_PROPERTY_OUTPUT_PATH));
    }

    @Override
    protected void validateConfigFileValues(final Properties propertiesFile) {
        super.validateConfigFileValues(propertiesFile);

        validateLibreOfficePath(propertiesFile);
    }

    private static void validateLibreOfficePath(final Properties properties) {
        final String libreOfficePath = properties.getProperty(KEY_PROPERTY_LIBRE_OFFICE);
        if (libreOfficePath == null || libreOfficePath.isEmpty()) {
            properties.setProperty(KEY_PROPERTY_LIBRE_OFFICE, "soffice");
            LOG.log(Level.WARNING, "Properties value for \"libreOfficePath\" was not set. Using a value of \"soffice\"");
        }

    }
}
