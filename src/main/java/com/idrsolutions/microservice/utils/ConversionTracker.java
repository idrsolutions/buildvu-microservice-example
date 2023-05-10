package com.idrsolutions.microservice.utils;

import com.idrsolutions.microservice.db.DBHandler;
import org.jpedal.external.ErrorTracker;

public class ConversionTracker implements ErrorTracker {

    private final String uuid;

    public ConversionTracker(final String uuid) {
        this.uuid = uuid;
    }

    public void addPageFailureMessage(String s) {

    }

    public String getPageFailureMessage() {
        return null;
    }


    public boolean ispageSuccessful() {
        return false;
    }


    public boolean checkForExitRequest(int dataPointer, int streamSize) {
        return false;
    }


    public void finishedPageDecoding(final int rawPage) {
        DBHandler.getInstance().setCustomValue(uuid, "pagesConverted", String.valueOf(rawPage));
    }


    public void startedPageDecoding(int i) {

    }
}
