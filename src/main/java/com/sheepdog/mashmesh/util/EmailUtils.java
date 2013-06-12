package com.sheepdog.mashmesh.util;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailUtils {
    private static final Session session = Session.getDefaultInstance(new Properties());

    public static String extractDomain(String emailAddress) {
        return emailAddress.substring(emailAddress.indexOf('@') + 1, emailAddress.length());
    }

    public static void sendEmail(String recipient, String subject, String htmlMessage, String bcc)
            throws MessagingException {
        InternetAddress senderAddress = new InternetAddress(ApplicationConfiguration.getNotificationEmailSender());
        InternetAddress recipientAddress = new InternetAddress(recipient);
        Message message = new MimeMessage(session);
        message.setFrom(senderAddress);
        message.addRecipient(Message.RecipientType.TO, recipientAddress);
        message.setSubject(subject);
        message.setContent(htmlMessage, "text/html");
        Transport.send(message);

        if (bcc != null) {
            sendEmail(bcc, subject, htmlMessage, null);
        }
    }

    public static void sendEmail(String recipient, String subject, String htmlMessage) throws MessagingException {
        sendEmail(recipient, subject, htmlMessage, null);
    }
}