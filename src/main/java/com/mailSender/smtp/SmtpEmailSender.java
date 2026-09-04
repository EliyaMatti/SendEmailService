package com.mailSender.smtp;

import com.mailSender.MailAppProperties;
import com.mailSender.MailBodyAttachment;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * SMTP implementation of {@link EmailSender}. Dry-run logs To + body and does not call {@link
 * JavaMailSender}.
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
  public void sendEmail(String to, String body) {
    if (mailAppProperties.isDryRun()) {
      log.info("DRY-RUN To: {}", to);
      log.info("{}", body);
      return;
    }
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true);
      helper.setFrom(mailAppProperties.getFrom());
      helper.setTo(to);
      helper.setSubject(mailAppProperties.getSubject());
      helper.setText(body, mailAppProperties.isHtml());
      mailBodyAttachment.addAttachment(helper);
      mailSender.send(message);
      log.info("Sent message successfully to {}", to);
    } catch (SmtpSendException e) {
      throw e;
    } catch (Exception e) {
      log.debug("SMTP send failed to {}", to, e);
      throw new SmtpSendException(SmtpFailureClassifier.userMessage(to, e), e);
    }
  }
}
