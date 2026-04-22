package com.buenws.buenws_backend.Util;

import com.buenws.buenws_backend.API.Exception.Custom.MailException;
import jakarta.mail.*;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

@Component
public class MailUtil {

    private final JavaMailSender mailSender;

    public MailUtil(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean SendOTPMail(String recipient, String subject, String newOTP){
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(recipient);
            helper.setSubject(subject);

            ClassPathResource resource = new ClassPathResource("templates/otp_template.html");
            String htmlTemplate = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            helper.setText(htmlTemplate.replace("{{OTP}}", newOTP), true);

            mailSender.send(message);
            return true;
        }catch (MessagingException e) {
            throw new MailException("Invalid Subject or Text","INVALID_MAIL",e);
        } catch (IOException | MailAuthenticationException e) {
            throw new MailException("Internal Error occurred. Please contact support under: info.buenows@gmail.com", "INVALID_MAIL", e);
        }
    }
}
