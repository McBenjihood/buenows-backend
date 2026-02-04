package com.buenws.buenws_backend.util;

import java.time.Instant;
import java.util.Date;

public class BuenowsUtil {
    public static Instant getCurrentDate(){
        return Instant.now();
    }
    public static Instant getHourFromNow(){
        return Instant.now().plusMillis(3600 * 1000);
    }
    public static Instant getWeekFromNow(){
        return Instant.now().plusMillis(604800 * 1000);
    }
}
