package com.mailSender.smtp;

import com.mailSender.MailAppProperties;
import com.mailSender.MailBodyAttachment;
import com.mailSender.campaign.EmailMessage;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * SMTP implementation of {@link EmailSender}. Builds MIME from {@link EmailMessage}; dry-run logs
 * To + body and does not call {@link JavaMailSender}.
 */
@Component
public class SmtpEmailSender implements EmailSender {

  private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

  private final JavaMailSender mailSender;
  private final MailAppProperties mailAppProperties;
  private final MailBodyAttachment mailBodyAttachment;

  public SmtpEmailSender(
      JavaMailSender mailSender,
      MailAppProperties mailAppProperties,
      MailBodyAttachment mailBodyAttachment) {
    this.mailSender = mailSender;
    this.mailAppProperties = mailAppProperties;
    this.mailBodyAttachment = mailBodyAttachment;
  }

  @Override
  public void send(EmailMessage message) {
    String to = message.getTo();
    if (mailAppProperties.isDryRun()) {
      log.info("DRY-RUN To: {}", to);
      log.info("{}", message.getBody());
      return;
    }
    try {
      MimeMessage mimeMessage = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
      helper.setFrom(message.getFrom());
      helper.setTo(to);
      helper.setSubject(message.getSubject());
      helper.setText(message.getBody(), mailAppProperties.isHtml());
      if (!message.getReplyTo().isBlank()) {
        helper.setReplyTo(message.getReplyTo());
      }
      mailBodyAttachment.addAttachments(helper, message.getAttachments());
      mailSender.send(mimeMessage);
      log.info("Sent message successfully to {}", to);
    } catch (SmtpSendException e) {
      throw e;
    } catch (Exception e) {
      log.debug("SMTP send failed to {}", to, e);
      throw new SmtpSendException(SmtpFailureClassifier.userMessage(to, e), e);
    }
  }
}
