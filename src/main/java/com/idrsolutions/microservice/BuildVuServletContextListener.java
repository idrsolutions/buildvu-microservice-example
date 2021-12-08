package com.idrsolutions.microservice;

import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;
import java.util.Properties;
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
        Properties propertiesFile = (Properties) servletContextEvent.getServletContext().getAttribute("properties");
        OutputFileServlet.setBasePath(propertiesFile.getProperty("outputPath"));
    }

    @Override
    public void validateConfigFileValues(final Properties propertiesFile) {

        super.validateConfigFileValues(propertiesFile);

        //service.libreOfficePath
        validateLibreOfficePath(propertiesFile);

    }

    private static void validateLibreOfficePath(final Properties properties) {
        final String libreOfficePath = properties.getProperty("libreOfficePath");
        if (libreOfficePath == null || libreOfficePath.isEmpty()) {
            properties.setProperty("libreOfficePath", "soffice");
        }

    }
}
