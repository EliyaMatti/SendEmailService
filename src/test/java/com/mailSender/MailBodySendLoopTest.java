package com.mailSender;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mailSender.excel.Contact;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MailBodySendLoopTest {

  @TempDir Path tempDir;

  @Test
  void oneSendFailureDoesNotStopLaterRecipients() throws Exception {
    Path body = tempDir.resolve("body.txt");
    Files.writeString(body, "Hi {{name}}", StandardCharsets.UTF_8);

    EmailService emailService = mock(EmailService.class);
    doThrow(new RuntimeException("smtp failed"))
        .when(emailService)
        .sendEmail(eq("a@example.com"), anyString());

    MailAppProperties properties = new MailAppProperties();
    properties.setSubject("Test subject");
    properties.setDryRun(false);
    properties.setSendDelayMs(0);
    SentAddressLog sentLog = mock(SentAddressLog.class);
    when(sentLog.load()).thenReturn(Set.of());

    MailBody mailBody = new MailBody(emailService, properties, sentLog);
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () ->
                mailBody.sendPersonalizedEmails(
                    body.toString(),
                    List.of(
                        new Contact("a@example.com", "Ada"),
                        new Contact("b@example.com", "Bob"))));
    assertTrue(ex.getMessage().contains("1 send failure"));

    verify(emailService).sendEmail(eq("a@example.com"), anyString());
    verify(emailService).sendEmail(eq("b@example.com"), anyString());
    verify(emailService, times(2)).sendEmail(anyString(), anyString());
    verify(sentLog).record("b@example.com");
    verify(sentLog, never()).record("a@example.com");
  }

  @Test
  void skipsAddressesInSentLog() throws Exception {
    Path body = tempDir.resolve("body.txt");
    Files.writeString(body, "Hi {{name}}", StandardCharsets.UTF_8);

    EmailService emailService = mock(EmailService.class);
    MailAppProperties properties = new MailAppProperties();
    properties.setSubject("Test subject");
    properties.setDryRun(false);
    properties.setSendDelayMs(0);
    SentAddressLog sentLog = mock(SentAddressLog.class);
    when(sentLog.load()).thenReturn(Set.of("a@example.com"));

    MailBody mailBody = new MailBody(emailService, properties, sentLog);
    mailBody.sendPersonalizedEmails(
        body.toString(),
        List.of(
            new Contact("a@example.com", "Ada"),
            new Contact("b@example.com", "Bob")));

    verify(emailService, never()).sendEmail(eq("a@example.com"), anyString());
    verify(emailService).sendEmail(eq("b@example.com"), anyString());
  }

  @Test
  void dryRunDoesNotRecordSentLog() throws Exception {
    Path body = tempDir.resolve("body.txt");
    Files.writeString(body, "Hi {{name}}", StandardCharsets.UTF_8);

    EmailService emailService = mock(EmailService.class);
    MailAppProperties properties = new MailAppProperties();
    properties.setSubject("Test subject");
    properties.setDryRun(true);
    properties.setSendDelayMs(1000);
    SentAddressLog sentLog = mock(SentAddressLog.class);
    when(sentLog.load()).thenReturn(Set.of());

    MailBody mailBody = new MailBody(emailService, properties, sentLog);
    mailBody.sendPersonalizedEmails(
        body.toString(), List.of(new Contact("a@example.com", "Ada")));

    verify(emailService).sendEmail(eq("a@example.com"), anyString());
    verify(sentLog, never()).record(anyString());
  }

  @Test
  void skipsInFileDuplicateAfterFirstSuccessIgnoringCase() throws Exception {
    Path body = tempDir.resolve("body.txt");
    Files.writeString(body, "Hi {{name}}", StandardCharsets.UTF_8);

    EmailService emailService = mock(EmailService.class);
    MailAppProperties properties = new MailAppProperties();
    properties.setSubject("Test subject");
    properties.setDryRun(false);
    properties.setSendDelayMs(0);
    SentAddressLog sentLog = mock(SentAddressLog.class);
    when(sentLog.load()).thenReturn(Set.of());

    MailBody mailBody = new MailBody(emailService, properties, sentLog);
    mailBody.sendPersonalizedEmails(
        body.toString(),
        List.of(
            new Contact("Ada@Example.com", "Ada"),
            new Contact("ada@example.com", "Ada again")));

    verify(emailService, times(1)).sendEmail(eq("Ada@Example.com"), anyString());
    verify(emailService, never()).sendEmail(eq("ada@example.com"), anyString());
    verify(sentLog).record("Ada@Example.com");
  }

  @Test
  void sentLogWriteFailureAfterSmtpSuccessIsNotASendFailure() throws Exception {
    Path body = tempDir.resolve("body.txt");
    Files.writeString(body, "Hi {{name}}", StandardCharsets.UTF_8);

    EmailService emailService = mock(EmailService.class);
    MailAppProperties properties = new MailAppProperties();
    properties.setSubject("Test subject");
    properties.setDryRun(false);
    properties.setSendDelayMs(0);
    SentAddressLog sentLog = mock(SentAddressLog.class);
    when(sentLog.load()).thenReturn(Set.of());
    doThrow(new IllegalStateException("disk full")).when(sentLog).record("a@example.com");

    MailBody mailBody = new MailBody(emailService, properties, sentLog);
    mailBody.sendPersonalizedEmails(
        body.toString(),
        List.of(
            new Contact("a@example.com", "Ada"),
            new Contact("b@example.com", "Bob")));

    verify(emailService).sendEmail(eq("a@example.com"), anyString());
    verify(emailService).sendEmail(eq("b@example.com"), anyString());
    verify(sentLog).record("a@example.com");
    verify(sentLog).record("b@example.com");
  }

  @Test
  void emptyRecipientListDoesNotSend() throws Exception {
    Path body = tempDir.resolve("body.txt");
    Files.writeString(body, "Hi {{name}}", StandardCharsets.UTF_8);

    EmailService emailService = mock(EmailService.class);
    MailAppProperties properties = new MailAppProperties();
    properties.setSubject("Test subject");
    properties.setDryRun(false);
    properties.setSendDelayMs(0);
    SentAddressLog sentLog = mock(SentAddressLog.class);

    MailBody mailBody = new MailBody(emailService, properties, sentLog);
    mailBody.sendPersonalizedEmails(body.toString(), List.of());

    verify(emailService, never()).sendEmail(anyString(), anyString());
    verify(sentLog, never()).load();
    verify(sentLog, never()).record(anyString());
  }

  @Test
  void unsupportedPlaceholderFailsBeforeAnySend() throws Exception {
    Path body = tempDir.resolve("body.txt");
    Files.writeString(body, "Welcome to {{Company}}.", StandardCharsets.UTF_8);

    EmailService emailService = mock(EmailService.class);
    MailAppProperties properties = new MailAppProperties();
    properties.setSubject("Test subject");
    properties.setDryRun(false);
    properties.setSendDelayMs(0);
    SentAddressLog sentLog = mock(SentAddressLog.class);

    MailBody mailBody = new MailBody(emailService, properties, sentLog);
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () ->
                mailBody.sendPersonalizedEmails(
                    body.toString(), List.of(new Contact("a@example.com", "Ada"))));
    assertTrue(ex.getMessage().contains("Placeholder {{Company}} does not exist"));
    verify(emailService, never()).sendEmail(anyString(), anyString());
  }
}
