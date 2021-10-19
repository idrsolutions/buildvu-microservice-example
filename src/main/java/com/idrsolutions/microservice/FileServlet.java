package com.idrsolutions.microservice;

import com.idrsolutions.microservice.utils.DefaultFileServlet;

import javax.servlet.annotation.WebServlet;

@WebServlet("/output/*")
public class FileServlet extends DefaultFileServlet {

    private static String basePath;

    public static void setBasePath(final String basePathParam) {
        basePath = basePathParam;
    }

    @Override
    protected String getBasePath() {
        return basePath;
    }
}
