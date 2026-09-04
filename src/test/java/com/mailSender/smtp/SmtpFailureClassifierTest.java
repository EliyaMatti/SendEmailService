package com.mailSender.smtp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;

class SmtpFailureClassifierTest {

  @Test
  void authenticationFailure() {
    String message =
        SmtpFailureClassifier.userMessage(
            "a@example.com", new MailAuthenticationException("535-5.7.8"));
    assertTrue(message.contains("authentication failed"));
    assertTrue(message.contains("MAIL_PASSWORD"));
    assertNoStackTrace(message);
  }

  @Test
  void authenticationFailedExceptionCause() {
    String message =
        SmtpFailureClassifier.userMessage(
            "a@example.com",
            new MailSendException("send", new AuthenticationFailedException("bad credentials")));
    assertTrue(message.contains("authentication failed"));
  }

  @Test
  void connectionFailure() {
    String message =
        SmtpFailureClassifier.userMessage(
            "a@example.com", new MailSendException("send", new ConnectException("Connection refused")));
    assertTrue(message.contains("Could not connect"));
    assertTrue(message.contains("MAIL_HOST"));
    assertNoStackTrace(message);
  }

  @Test
  void unknownHost() {
    String message =
        SmtpFailureClassifier.userMessage(
            "a@example.com", new UnknownHostException("smtp.example.invalid"));
    assertTrue(message.contains("Could not connect"));
  }

  @Test
  void timeout() {
    String message =
        SmtpFailureClassifier.userMessage(
            "a@example.com", new SocketTimeoutException("Read timed out"));
    assertTrue(message.contains("timed out"));
    assertTrue(message.contains("a@example.com"));
    assertNoStackTrace(message);
  }

  @Test
  void invalidRecipient() throws Exception {
    SendFailedException sendFailed =
        new SendFailedException(
            "invalid",
            new Exception("bad rcpt"),
            null,
            null,
            new InternetAddress[] {new InternetAddress("a@example.com", false)});
    String message = SmtpFailureClassifier.userMessage("a@example.com", sendFailed);
    assertTrue(message.contains("rejected recipient"));
    assertTrue(message.contains("a@example.com"));
  }

  @Test
  void addressExceptionIsInvalidRecipient() {
    String message =
        SmtpFailureClassifier.userMessage(
            "not-an-email", new AddressException("Local address contains control or whitespace"));
    assertTrue(message.contains("rejected recipient"));
  }

  @Test
  void smtpRejection() {
    String message =
        SmtpFailureClassifier.userMessage("a@example.com", new MailSendException("554 Relay denied"));
    assertTrue(message.contains("rejected the message"));
    assertNoStackTrace(message);
  }

  @Test
  void configurationErrorFromParse() {
    String message =
        SmtpFailureClassifier.userMessage(
            "a@example.com", new MailParseException("Failed to parse address"));
    assertTrue(message.startsWith("Unable to send the message because of an SMTP configuration problem"));
    assertFalse(message.contains("Failed to parse address"));
  }

  @Test
  void configurationErrorFromMissingAttachment() {
    String message =
        SmtpFailureClassifier.userMessage(
            "a@example.com", new RuntimeException("Cannot read file: C:\\missing.pdf"));
    assertTrue(message.contains("SMTP configuration problem"));
    assertFalse(message.contains("C:\\missing.pdf"));
  }

  @Test
  void unknownFailureKeepsShortMessage() {
    String message =
        SmtpFailureClassifier.userMessage("a@example.com", new IllegalStateException("boom"));
    assertEquals("Unable to send email to a@example.com.", message);
  }

  private static void assertNoStackTrace(String message) {
    assertFalse(message.contains("\n\tat "));
    assertFalse(message.contains("Exception in thread"));
  }
}
