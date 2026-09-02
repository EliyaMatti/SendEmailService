package com.mailSender;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  private final JavaMailSender mailSender;
  private final MailAppProperties mailAppProperties;
  private final MailBodyAttachment mailBodyAttachment;

  public EmailService(
      JavaMailSender mailSender,
      MailAppProperties mailAppProperties,
      MailBodyAttachment mailBodyAttachment) {
    this.mailSender = mailSender;
    this.mailAppProperties = mailAppProperties;
    this.mailBodyAttachment = mailBodyAttachment;
  }

  public void sendEmail(String to, String body) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true);
      helper.setFrom(mailAppProperties.getFrom());
      helper.setTo(to);
      helper.setSubject(mailAppProperties.getSubject());
      helper.setText(body);
      mailBodyAttachment.addAttachment(helper);
      mailSender.send(message);
      System.out.println("Sent message successfully to " + to);
    } catch (Exception e) {
      throw new RuntimeException("Failed to send email to " + to, e);
    }
  }
}
