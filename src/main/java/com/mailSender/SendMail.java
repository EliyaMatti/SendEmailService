
package com.mailSender;

import java.util.Properties;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class SendMail {

private static void sendEmail(String from, String to, String body) {
	String host = "smtp.gmail.com";
    int port = 587;
	
    // Setup mail server
  Properties props = new Properties();
  props.put("mail.smtp.auth", "true");
  props.put("mail.smtp.starttls.enable", "true");
  props.put("mail.smtp.host", host);
  props.put("mail.smtp.port", port); // Adjust port if necessary

    // Get the default Session object
    Session session = Session.getDefaultInstance(props);

    try {
        // Create a default MimeMessage object
        MimeMessage message = new MimeMessage(session);

        // Set From: header field
        message.setFrom(new InternetAddress(from));

        // Set To: header field
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

        // Set Subject: header field
        message.setSubject("Subject: Personalized Greeting");

        // Set the actual message
        message.setText(body);
       
        // Send message
        Transport.send(message);
        System.out.println("Sent message successfully to " + to);
    } catch (MessagingException mex) {
        mex.printStackTrace();
    }
}
}
