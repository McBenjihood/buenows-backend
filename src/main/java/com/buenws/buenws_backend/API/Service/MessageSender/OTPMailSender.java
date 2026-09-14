package com.buenws.buenws_backend.API.Service.MessageSender;

import com.buenws.buenws_backend.API.Exception.Custom.MailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class OTPMailSender implements OTPMessageSender {

    @Value("${app.mail.from}")
    private String fromEmail;
    private final JavaMailSender mailSender;

    public OTPMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private enum SupportedLanguage {
        EN(Locale.US),
        DE(Locale.GERMAN);

        private final Locale locale;

        SupportedLanguage(Locale locale) {
            this.locale = locale;
        }

        public Locale getLocale() {
            return locale;
        }
    }

    private Locale getLocale(String languageTag) {
        if (languageTag != null && languageTag.trim().toLowerCase().startsWith("en")) {
            return SupportedLanguage.EN.getLocale();
        }
        return SupportedLanguage.DE.getLocale();
    }


    public boolean SendMessage(String recipient, String newOTP, String language_tag) {
        Locale locale = getLocale(language_tag);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            String safeRecipient = recipient == null ? "" : recipient;
            String safeOTP = newOTP == null ? "" : newOTP;

            OtpMailCopy copy = resolveCopy(locale, safeOTP);

            helper.setTo(safeRecipient);
            helper.setSubject(copy.subject());
            helper.setText(renderTemplate(safeOTP, locale), true);
            helper.setFrom(fromEmail);


            System.out.println("Sending mail from: " + fromEmail);
            mailSender.send(message);
            return true;
        } catch (MessagingException e) {
            throw new MailException("Invalid Subject or Text", "INVALID_MAIL", e);
        } catch (IOException | MailAuthenticationException e) {
            throw new MailException(
                    "Internal Error occurred. Please contact support under: info.buenows@gmail.com",
                    "INVALID_MAIL",
                    e
            );
        }
    }

    String renderTemplate(String safeOTP, Locale locale) throws IOException {
        OtpMailCopy copy = resolveCopy(locale,safeOTP);

        ClassPathResource resource = new ClassPathResource("templates/otp_template.html");
        String htmlTemplate = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        return htmlTemplate
                .replace("{{HTML_LANG}}", copy.htmlLanguage())
                .replace("{{TITLE}}", copy.title())
                .replace("{{BRAND}}", "Bueno Web Solutions")
                .replace("{{HEADING}}", copy.heading())
                .replace("{{OTP}}", safeOTP)
                .replace("{{INFO_TEXT}}", copy.infoText())
                .replace("{{COPYRIGHT}}", copy.copyright())
                .replace("{{PRIVACY_LABEL}}", copy.privacyLabel())
                .replace("{{SUPPORT_LABEL}}", copy.supportLabel())
                .replace("{{REASON_TEXT}}", copy.reasonText());
    }

    OtpMailCopy resolveCopy(Locale locale, String safe_otp) {
        String language = locale == null ? "de" : locale.getLanguage();

        if ("en".equalsIgnoreCase(language)) {
            return new OtpMailCopy(
                    "en",
                    "Your Bueno Web Solutions verification code is " + safe_otp,
                    "Verification code",
                    "Your verification code",
                    "This code will expire in <strong>15 minutes</strong>. If you did not request this, you can safely ignore this email.",
                    "&copy; 2026 Bueno Web Solutions. All rights reserved.",
                    "Privacy",
                    "Support",
                    "You are receiving this because a verification code was requested for your Bueno Web Solutions account."
            );
        }

        return new OtpMailCopy(
                "de",
                "Ihr Bueno Web Solutions Bestätigungscode ist " + safe_otp,
                "Bestätigungscode",
                "Ihr Bestätigungscode",
                "Dieser Code läuft in <strong>15 Minuten</strong> ab. Wenn Sie ihn nicht angefordert haben, können Sie diese E-Mail ignorieren.",
                "&copy; 2026 Bueno Web Solutions. Alle Rechte vorbehalten.",
                "Datenschutz",
                "Support",
                "Sie erhalten diese E-Mail, weil ein Bestätigungscode für Ihr Bueno Web Solutions Konto angefordert wurde."
        );
    }

    record OtpMailCopy(
            String htmlLanguage,
            String subject,
            String title,
            String heading,
            String infoText,
            String copyright,
            String privacyLabel,
            String supportLabel,
            String reasonText
    ) {
    }
}
