package com.buenws.buenws_backend.API.Service.MessageSender;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OTPMailSenderTest {

    @Test
    void renderTemplateUsesGermanCopyForGermanLocale() throws IOException {
        OTPMailSender sender = new OTPMailSender(null);

        String html = sender.renderTemplate("123456", Locale.GERMAN);

        assertTrue(html.contains("Ihr Bestätigungscode"));
        assertTrue(html.contains("Dieser Code läuft"));
        assertTrue(html.contains("123456"));
    }

    @Test
    void renderTemplateUsesEnglishCopyForEnglishLocale() throws IOException {
        OTPMailSender sender = new OTPMailSender(null);

        String html = sender.renderTemplate("654321", Locale.ENGLISH);

        assertTrue(html.contains("Your verification code"));
        assertTrue(html.contains("This code will expire"));
        assertTrue(html.contains("654321"));
    }
}
