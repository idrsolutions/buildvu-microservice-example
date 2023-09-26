package com.idrsolutions.microservice.utils;

import com.idrsolutions.microservice.db.DBHandler;
import org.jpedal.external.RemoteTracker;

public class ConversionTracker implements RemoteTracker {

    private final String uuid;

    public ConversionTracker(final String uuid) {
        this.uuid = uuid;
    }

    @Override
    public void finishedPageDecoding(final int rawPage) {
        DBHandler.getInstance().setCustomValue(uuid, "pagesConverted", String.valueOf(rawPage));
    }

    @Override
    public void startedPageDecoding(final int i) {
        // Page progress update not needed on page started decoding
    }
}
