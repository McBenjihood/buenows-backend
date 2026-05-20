package com.buenws.buenws_backend.API.Service.MessageSender;

import java.util.Locale;

public interface OTPMessageSender {
    boolean SendMessage(String recipient, String new_otp, Locale locale);
}
