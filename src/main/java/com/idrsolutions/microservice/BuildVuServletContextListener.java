package com.idrsolutions.microservice;

import javax.servlet.annotation.WebListener;

@WebListener
public class BuildVuServletContextListener extends BaseServletContextListener {

    public String getConfigPath() {
        String userDir = System.getProperty("user.home");
        if (!userDir.endsWith("/") && !userDir.endsWith("\\")) {
            userDir += System.getProperty("file.separator");
        }
        return userDir + "/.idr/buildvu-microservice/";
    }

    public String getConfigName(){
        return "buildvu-microservice.properties";
    }
}
