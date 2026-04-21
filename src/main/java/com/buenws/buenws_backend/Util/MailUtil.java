package com.buenws.buenws_backend.Util;

import com.buenws.buenws_backend.API.Exception.Custom.MailException;
import jakarta.mail.*;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;

import java.util.Properties;

public class MailUtil {

    @Value("${email.password}")
    private static String password;

    public static void SendOTPMail(String recipient, String subject, String content){
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("info.buenows@gmail.com", password);
            }
        });

        Message message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress("info.buenows@gmail.com"));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(recipient));
            message.setSubject(subject);
            message.setContent(content, "text/html");
            Transport.send(message);
        }catch (AddressException e) {
            throw new MailException("Invalid Recipient", "INVALID_MAIL", e);
        } catch (MessagingException e) {
            throw new MailException("Invalid Subject or Text","INVALID_MAIL",e);
        }
    }


}
