package com.buenws.buenws_backend.Util;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class CryptographyUtil {
    public static String HashString(String originalString, String salt){
        return Hashing.sha256()
                .hashString(originalString + salt, StandardCharsets.UTF_8)
                .toString();
    }

    public static String generateOTP(){
        SecureRandom rnd = new SecureRandom();
        int number = rnd.nextInt(999999);
        return String.format("%06d", number);
    }
}
