package com.buenws.buenws_backend.Util;

import java.time.Instant;

public class TimeUtil {
    public static Instant getCurrentDate(){
        return Instant.now();
    }
    public static Instant get15MinutesFromNow(){
        return Instant.now().plusMillis(900 * 1000);
    }
    public static Instant getHourFromNow(){
        return Instant.now().plusMillis(3600 * 1000);
    }
    public static Instant getWeekFromNow(){
        return Instant.now().plusMillis(604800 * 1000);
    }
}
