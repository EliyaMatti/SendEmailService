package com.mailSender;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

class EmailServiceDryRunTest {

  @Test
  void dryRunDoesNotCallJavaMailSender() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    MailBodyAttachment attachment = mock(MailBodyAttachment.class);
    MailAppProperties properties = new MailAppProperties();
    properties.setDryRun(true);

    EmailService emailService = new EmailService(mailSender, properties, attachment);
    emailService.sendEmail("a@example.com", "Hello {{name}}");

    verifyNoInteractions(mailSender);
    verifyNoInteractions(attachment);
  }
}
