package com.mailSender;

import java.util.Properties;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

public class MessageBody {
	
	public static void sendEmail(String from, String to, String body) {
        // Setup mail server
		
		 String fromEmail = "eliyamatti1@gmail.com";
	        String password = "hjij ahpi whbd nzwo";
		String host = "smtp.gmail.com";
	    int port = 587;
		Properties props = new Properties();
		  props.put("mail.smtp.auth", "true");
		  props.put("mail.smtp.starttls.enable", "true");
		  props.put("mail.smtp.host", host);
		  props.put("mail.smtp.port", port); // Adjust port if necessary

		    // Get the default Session object
		    //Session session = Session.getDefaultInstance(props);

		    Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
	            protected PasswordAuthentication getPasswordAuthentication() {
	                return new PasswordAuthentication(fromEmail, password);
	            }
	        });

        try {
            // Create a default MimeMessage object
            MimeMessage message = new MimeMessage(session);

            // Set From: header field
            message.setFrom(new InternetAddress(from));

            // Set To: header field
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

            // Set Subject: header field
            message.setSubject("Java developer Application – Eliya");
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(body);
            MimeBodyPart attachmentBodyPart = MailBodyAttachment.attachemt();

			Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            multipart.addBodyPart(attachmentBodyPart);
           
            message.setContent(multipart);

            // Send message
            Transport.send(message);
            System.out.println("Sent message successfully to " + to);
        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }

}
