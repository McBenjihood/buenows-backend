package com.buenws.buenws_backend.Util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileUtil {
    public static String getFilePrefix() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    public static void convertToPng(String Filepath){

    }
}
