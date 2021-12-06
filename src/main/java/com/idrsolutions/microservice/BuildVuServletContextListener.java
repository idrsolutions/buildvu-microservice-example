package com.idrsolutions.microservice;

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
    public void validateConfigFileValues(final Properties propertiesFile) {

        super.validateConfigFileValues(propertiesFile);

        //service.libreOfficePath
        validateLibreOfficePath(propertiesFile);

    }

    private static void validateLibreOfficePath(final Properties properties) {
        final String libreOfficePath = properties.getProperty("service.libreOfficePath");
        if (libreOfficePath == null || libreOfficePath.isEmpty()) {
            properties.setProperty("service.libreOfficePath", "soffice");
        }

    }
}
