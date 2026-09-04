package com.mailSender.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mailSender.config.MailAppProperties;
import com.mailSender.MailBodyAttachment;
import com.mailSender.campaign.EmailMessage;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;

/** SMTP failures become {@link SmtpSendException} with a short operator message. */
class SmtpEmailSenderErrorTest {

  @Test
  void mapsSmtpFailuresToUserFacingExceptionWithoutStackTrace() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    doThrow(new MailAuthenticationException("535-5.7.8 Username and Password not accepted"))
        .when(mailSender)
        .send(any(MimeMessage.class));

    MailAppProperties properties = new MailAppProperties();
    properties.setDryRun(false);
    properties.setFrom("from@example.com");
    properties.setSubject("Hi");

    SmtpEmailSender sender =
        new SmtpEmailSender(mailSender, properties, mock(MailBodyAttachment.class));

    SmtpSendException ex =
        assertThrows(
            SmtpSendException.class,
            () ->
                sender.send(
                    new EmailMessage(
                        "a@example.com", "Hi", "body", "from@example.com", "", List.of())));
    assertTrue(ex.getMessage().contains("authentication failed"));
    assertFalse(ex.getMessage().contains("\n\tat "));
    assertEquals(MailAuthenticationException.class, ex.getCause().getClass());
  }
}
