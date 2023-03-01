package com.idrsolutions.microservice.utils;

import com.idrsolutions.microservice.db.DBHandler;
import org.jpedal.io.DefaultErrorTracker;

public class ConversionTracker extends DefaultErrorTracker {

    private final String uuid;
    private final long startTime;
    private final long maxDuration;

    public ConversionTracker(final String uuid, final long maxDuration) {
        this.uuid = uuid;
        this.maxDuration = maxDuration;
        startTime = System.currentTimeMillis();
    }

    @Override
    public boolean checkForExitRequest(int dataPointer, int streamSize) {
        if (System.currentTimeMillis() - startTime > maxDuration) {
            DBHandler.getInstance().setError(uuid, 1230, "Conversion exceeded max duration of " + maxDuration + "ms");
            return true;
        }

        return false;
    }

    @Override
    public void finishedPageDecoding(final int rawPage) {
        DBHandler.getInstance().setCustomValue(uuid, "pagesConverted", String.valueOf(rawPage));
    }
}
