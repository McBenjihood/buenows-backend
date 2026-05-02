package com.buenws.buenws_backend.Util;

import com.buenws.buenws_backend.API.Exception.Custom.MailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class MailUtil {

    private final JavaMailSender mailSender;

    public MailUtil(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean SendOTPMail(String recipient, String subject, String newOTP, String first_name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            String safeRecipient = recipient == null ? "" : recipient;
            String safeSubject = subject == null ? "Password reset" : subject;
            String safeOTP = newOTP == null ? "" : newOTP;
            String safeFirstName = first_name == null || first_name.isBlank() ? "there" : first_name;

            helper.setTo(safeRecipient);
            helper.setSubject(safeSubject);

            ClassPathResource resource = new ClassPathResource("templates/otp_template.html");
            String htmlTemplate = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            htmlTemplate = htmlTemplate.replace("{{OTP}}", safeOTP);
            htmlTemplate = htmlTemplate.replace("{{first_name}}", safeFirstName);

            helper.setText(htmlTemplate, true);

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
}