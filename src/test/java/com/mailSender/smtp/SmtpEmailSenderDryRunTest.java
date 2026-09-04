package com.mailSender.smtp;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.mailSender.config.MailAppProperties;
import com.mailSender.MailBodyAttachment;
import com.mailSender.campaign.EmailMessage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

/** Dry-run must not touch JavaMailSender or attachments. */
class SmtpEmailSenderDryRunTest {

  @Test
  void dryRunDoesNotCallJavaMailSender() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    MailBodyAttachment attachment = mock(MailBodyAttachment.class);
    MailAppProperties properties = new MailAppProperties();
    properties.setDryRun(true);

    EmailSender emailSender = new SmtpEmailSender(mailSender, properties, attachment);
    emailSender.send(
        new EmailMessage("a@example.com", "Hi", "Hello {{name}}", "from@example.com", "", List.of()));

    verifyNoInteractions(mailSender);
    verifyNoInteractions(attachment);
  }
}
