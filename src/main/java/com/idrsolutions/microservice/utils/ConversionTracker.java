/*
 * BuildVu Microservice Example
 *
 * Project Info: https://github.com/idrsolutions/base-microservice-example
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
