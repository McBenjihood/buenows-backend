package com.buenws.buenws_backend.API.Service.MessageSender;

public interface OTPMessageSender {
    public boolean SendMessage(String recipient, String subject, String new_otp);
}
