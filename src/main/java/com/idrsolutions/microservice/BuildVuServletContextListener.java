package com.idrsolutions.microservice;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.annotation.WebListener;

@WebListener
public class BuildVuServletContextListener extends BaseServletContextListener {

    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
        String userDir = System.getProperty("user.home");
        if (!userDir.endsWith("/") && !userDir.endsWith("\\")) {
            userDir += System.getProperty("file.separator");
        }
        propertiesFile = userDir + "/.idr/buildvu-microservice/buildvu-microservice.properties";

        super.contextInitialized(servletContextEvent);
    }

}
