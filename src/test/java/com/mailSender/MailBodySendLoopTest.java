package com.mailSender;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mailSender.campaign.EmailMessage;
import com.mailSender.excel.Contact;
import com.mailSender.smtp.EmailSender;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Campaign loop tests: one failure continues, sent-log skip, dry-run does not record. */
class MailBodySendLoopTest {

  @TempDir Path tempDir;

  @Test
  void oneSendFailureDoesNotStopLaterRecipients() throws Exception {
    Path body = tempDir.resolve("body.txt");
    Files.writeString(body, "Hi {{name}}", StandardCharsets.UTF_8);

    EmailSender emailSender = mock(EmailSender.class);
    doThrow(new RuntimeException("smtp failed"))
        .when(emailSender)
        .send(argThat(m -> m != null && "a@example.com".equals(m.getTo())));

    MailAppProperties properties = new MailAppProperties();
    properties.setSubject("Test subject");
    properties.setDryRun(false);
    properties.setSendDelayMs(0);
    SentAddressLog sentLog = mock(SentAddressLog.class);
    when(sentLog.load()).thenReturn(Set.of());

    MailBody mailBody = new MailBody(emailSender, properties, sentLog);
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

    verify(emailSender).send(argThat(m -> "a@example.com".equals(m.getTo())));
    verify(emailSender).send(argThat(m -> "b@example.com".equals(m.getTo())));
    verify(emailSender, times(2)).send(any(EmailMessage.class));
    verify(sentLog).record("b@example.com");
    verify(sentLog, never()).record("a@example.com");
  }

  @Test
  void skipsAddressesInSentLog() throws Exception {
    Path body = tempDir.resolve("body.txt");
    Files.writeString(body, "Hi {{name}}", StandardCharsets.UTF_8);

    EmailSender emailSender = mock(EmailSender.class);
    MailAppProperties properties = new MailAppProperties();
    properties.setSubject("Test subject");
    properties.setDryRun(false);
    properties.setSendDelayMs(0);
    SentAddressLog sentLog = mock(SentAddressLog.class);
    when(sentLog.load()).thenReturn(Set.of("a@example.com"));

    MailBody mailBody = new MailBody(emailSender, properties, sentLog);
    mailBody.sendPersonalizedEmails(
        body.toString(),
        List.of(
            new Contact("a@example.com", "Ada"),
            new Contact("b@example.com", "Bob")));

    verify(emailSender, never()).send(argThat(m -> m != null && "a@example.com".equals(m.getTo())));
    verify(emailSender).send(argThat(m -> "b@example.com".equals(m.getTo())));
  }

  @Test
  void dryRunDoesNotRecordSentLog() throws Exception {
    Path body = tempDir.resolve("body.txt");
    Files.writeString(body, "Hi {{name}}", StandardCharsets.UTF_8);

    EmailSender emailSender = mock(EmailSender.class);
    MailAppProperties properties = new MailAppProperties();
    properties.setSubject("Test subject");
    properties.setDryRun(true);
    properties.setSendDelayMs(1000);
    SentAddressLog sentLog = mock(SentAddressLog.class);
    when(sentLog.load()).thenReturn(Set.of());

    MailBody mailBody = new MailBody(emailSender, properties, sentLog);
    mailBody.sendPersonalizedEmails(
        body.toString(), List.of(new Contact("a@example.com", "Ada")));

    verify(emailSender)
        .send(
            argThat(
                m ->
                    "a@example.com".equals(m.getTo())
                        && "Test subject".equals(m.getSubject())
                        && m.getBody().contains("Ada")));
    verify(sentLog, never()).record(any());
  }

  @Test
  void skipsInFileDuplicateAfterFirstSuccessIgnoringCase() throws Exception {
    Path body = tempDir.resolve("body.txt");
    Files.writeString(body, "Hi {{name}}", StandardCharsets.UTF_8);

    EmailSender emailSender = mock(EmailSender.class);
    MailAppProperties properties = new MailAppProperties();
    properties.setSubject("Test subject");
    properties.setDryRun(false);
    properties.setSendDelayMs(0);
    SentAddressLog sentLog = mock(SentAddressLog.class);
    when(sentLog.load()).thenReturn(Set.of());

    MailBody mailBody = new MailBody(emailSender, properties, sentLog);
    mailBody.sendPersonalizedEmails(
        body.toString(),
        List.of(
            new Contact("Ada@Example.com", "Ada"),
            new Contact("ada@example.com", "Ada again")));

    verify(emailSender, times(1)).send(argThat(m -> "Ada@Example.com".equals(m.getTo())));
    verify(emailSender, never()).send(argThat(m -> m != null && "ada@example.com".equals(m.getTo())));
    verify(sentLog).record("Ada@Example.com");
  }

  @Test
  void sentLogWriteFailureAfterSmtpSuccessIsNotASendFailure() throws Exception {
    Path body = tempDir.resolve("body.txt");
    Files.writeString(body, "Hi {{name}}", StandardCharsets.UTF_8);

    EmailSender emailSender = mock(EmailSender.class);
    MailAppProperties properties = new MailAppProperties();
    properties.setSubject("Test subject");
    properties.setDryRun(false);
    properties.setSendDelayMs(0);
    SentAddressLog sentLog = mock(SentAddressLog.class);
    when(sentLog.load()).thenReturn(Set.of());
    doThrow(new IllegalStateException("disk full")).when(sentLog).record("a@example.com");

    MailBody mailBody = new MailBody(emailSender, properties, sentLog);
    mailBody.sendPersonalizedEmails(
        body.toString(),
        List.of(
            new Contact("a@example.com", "Ada"),
            new Contact("b@example.com", "Bob")));

    verify(emailSender).send(argThat(m -> "a@example.com".equals(m.getTo())));
    verify(emailSender).send(argThat(m -> "b@example.com".equals(m.getTo())));
    verify(sentLog).record("a@example.com");
    verify(sentLog).record("b@example.com");
  }

  @Test
  void emptyRecipientListDoesNotSend() throws Exception {
    Path body = tempDir.resolve("body.txt");
    Files.writeString(body, "Hi {{name}}", StandardCharsets.UTF_8);

    EmailSender emailSender = mock(EmailSender.class);
    MailAppProperties properties = new MailAppProperties();
    properties.setSubject("Test subject");
    properties.setDryRun(false);
    properties.setSendDelayMs(0);
    SentAddressLog sentLog = mock(SentAddressLog.class);

    MailBody mailBody = new MailBody(emailSender, properties, sentLog);
    mailBody.sendPersonalizedEmails(body.toString(), List.of());

    verify(emailSender, never()).send(any(EmailMessage.class));
    verify(sentLog, never()).load();
    verify(sentLog, never()).record(any());
  }

  @Test
  void unsupportedPlaceholderFailsBeforeAnySend() throws Exception {
    Path body = tempDir.resolve("body.txt");
    Files.writeString(body, "Welcome to {{Company}}.", StandardCharsets.UTF_8);

    EmailSender emailSender = mock(EmailSender.class);
    MailAppProperties properties = new MailAppProperties();
    properties.setSubject("Test subject");
    properties.setDryRun(false);
    properties.setSendDelayMs(0);
    SentAddressLog sentLog = mock(SentAddressLog.class);

    MailBody mailBody = new MailBody(emailSender, properties, sentLog);
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () ->
                mailBody.sendPersonalizedEmails(
                    body.toString(), List.of(new Contact("a@example.com", "Ada"))));
    assertTrue(ex.getMessage().contains("Placeholder {{Company}} does not exist"));
    verify(emailSender, never()).send(any(EmailMessage.class));
  }
}
