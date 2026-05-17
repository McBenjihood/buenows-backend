package com.buenws.buenws_backend.API.Service.MessageSender;

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
public class OTPMailSender implements OTPMessageSender {

    private final JavaMailSender mailSender;

    public OTPMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean SendMessage(String recipient, String subject, String newOTP) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            String safeRecipient = recipient == null ? "" : recipient;
            String safeSubject = subject == null ? "Password reset" : subject;
            String safeOTP = newOTP == null ? "" : newOTP;

            helper.setTo(safeRecipient);
            helper.setSubject(safeSubject);

            ClassPathResource resource = new ClassPathResource("templates/otp_template.html");
            String htmlTemplate = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            htmlTemplate = htmlTemplate.replace("{{OTP}}", safeOTP);

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
