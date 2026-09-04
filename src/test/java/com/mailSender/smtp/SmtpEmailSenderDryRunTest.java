package com.mailSender.smtp;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.mailSender.MailAppProperties;
import com.mailSender.MailBodyAttachment;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpEmailSenderDryRunTest {

  @Test
  void dryRunDoesNotCallJavaMailSender() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    MailBodyAttachment attachment = mock(MailBodyAttachment.class);
    MailAppProperties properties = new MailAppProperties();
    properties.setDryRun(true);

    EmailSender emailSender = new SmtpEmailSender(mailSender, properties, attachment);
    emailSender.sendEmail("a@example.com", "Hello {{name}}");

    verifyNoInteractions(mailSender);
    verifyNoInteractions(attachment);
  }
}
