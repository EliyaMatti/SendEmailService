package com.mailSender.smtp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mailSender.MailBodyAttachment;
import com.mailSender.campaign.EmailMessage;
import com.mailSender.config.MailAppProperties;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

/** {@link SmtpEmailSender} tests: mock {@link JavaMailSender}; never open a real SMTP session. */
class SmtpEmailSenderTest {

  @Test
  @DisplayName("Successful send")
  void successfulSendCallsJavaMailSender() throws Exception {
    JavaMailSender mailSender = mockMailSender();
    MailBodyAttachment attachment = mock(MailBodyAttachment.class);
    SmtpEmailSender sender = sender(mailSender, attachment);

    EmailMessage message = sampleMessage();
    sender.send(message);

    verify(mailSender).send(any(MimeMessage.class));
    verify(attachment).addAttachments(any(), eq(List.of()));
  }

  @Test
  @DisplayName("Authentication failure")
  void authenticationFailureBecomesSmtpSendException() {
    SmtpSendException ex =
        sendFailure(new MailAuthenticationException("535-5.7.8 Username and Password not accepted"));
    assertTrue(ex.getMessage().contains("authentication failed"));
    assertNoStackInMessage(ex);
  }

  @Test
  @DisplayName("Connection failure")
  void connectionFailureBecomesSmtpSendException() {
    SmtpSendException ex =
        sendFailure(new MailSendException("send", new ConnectException("Connection refused")));
    assertTrue(ex.getMessage().contains("Could not connect"));
    assertNoStackInMessage(ex);
  }

  @Test
  @DisplayName("Invalid recipient")
  void invalidRecipientBecomesSmtpSendException() {
    SmtpSendException ex =
        sendFailure(new MailSendException("invalid addresses", new AddressException("Illegal address")));
    assertTrue(ex.getMessage().contains("rejected recipient"));
    assertTrue(ex.getMessage().contains("a@example.com"));
    assertNoStackInMessage(ex);
  }

  @Test
  @DisplayName("Timeout")
  void timeoutBecomesSmtpSendException() {
    SmtpSendException ex =
        sendFailure(new MailSendException("timed out", new SocketTimeoutException("Read timed out")));
    assertTrue(ex.getMessage().contains("timed out"));
    assertTrue(ex.getMessage().contains("a@example.com"));
    assertNoStackInMessage(ex);
  }

  @Test
  @DisplayName("Provider rejection")
  void providerRejectionBecomesSmtpSendException() throws Exception {
    SendFailedException rejected =
        new SendFailedException(
            "554 Relay denied",
            new Exception("rejected"),
            null,
            null,
            new InternetAddress[0]);
    SmtpSendException ex = sendFailure(new MailSendException("554 Relay denied", rejected));
    assertTrue(ex.getMessage().contains("rejected the message"));
    assertNoStackInMessage(ex);
  }

  private static SmtpSendException sendFailure(Exception smtpError) {
    JavaMailSender mailSender = mockMailSender();
    doThrow(smtpError).when(mailSender).send(any(MimeMessage.class));
    SmtpEmailSender sender = sender(mailSender, mock(MailBodyAttachment.class));
    return assertThrows(SmtpSendException.class, () -> sender.send(sampleMessage()));
  }

  private static JavaMailSender mockMailSender() {
    JavaMailSender mailSender = mock(JavaMailSender.class);
    when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    return mailSender;
  }

  private static SmtpEmailSender sender(JavaMailSender mailSender, MailBodyAttachment attachment) {
    MailAppProperties properties = new MailAppProperties();
    properties.setDryRun(false);
    properties.setHtml(false);
    return new SmtpEmailSender(mailSender, properties, attachment);
  }

  private static EmailMessage sampleMessage() {
    return new EmailMessage(
        "a@example.com", "Hi", "body", "from@example.com", "", List.of());
  }

  private static void assertNoStackInMessage(SmtpSendException ex) {
    assertFalse(ex.getMessage().contains("\n\tat "));
  }
}
