package com.mailSender;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class SendMail {

  private static void sendEmail(String from, String to, String body) {
    String host = "smtp.gmail.com";
    int port = 587;

    Properties props = new Properties();
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.host", host);
    props.put("mail.smtp.port", port);

    Session session = Session.getDefaultInstance(props);

    try {

      MimeMessage message = new MimeMessage(session);

      message.setFrom(new InternetAddress(from));

      message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));

      message.setSubject("Subject: Personalized Greeting");

      message.setText(body);

      Transport.send(message);
      System.out.println("Sent message successfully to " + to);
    } catch (MessagingException mex) {
      mex.printStackTrace();
    }
  }
}
