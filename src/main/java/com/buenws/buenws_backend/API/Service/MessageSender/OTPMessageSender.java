package com.buenws.buenws_backend.API.Service.MessageSender;

import java.util.Locale;

public interface OTPMessageSender {
    public boolean SendMessage(String recipient, String newOTP, String language_tag);
}
