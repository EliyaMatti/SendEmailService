package com.mailSender;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Test-send: one composed message, no Excel list, no sent-log write. */
class MailBodyTestSendTest {

  @TempDir Path tempDir;

  @Test
  void sendTestEmailSendsOnceAndDoesNotRecordSentLog() throws Exception {
    Path body = tempDir.resolve("body.txt");
    Files.writeString(body, "Hi {{name}}", StandardCharsets.UTF_8);

    EmailSender emailSender = mock(EmailSender.class);
    MailAppProperties properties = new MailAppProperties();
    properties.setSubject("Test subject");
    properties.setFrom("from@example.com");
    properties.setDryRun(false);
    SentAddressLog sentLog = mock(SentAddressLog.class);

    MailBody mailBody = new MailBody(emailSender, properties, sentLog);
    assertTrue(mailBody.sendTestEmail(body.toString(), new Contact("test@example.com", "Ada")));

    verify(emailSender, times(1))
        .send(
            argThat(
                m ->
                    "test@example.com".equals(m.getTo())
                        && "Hi Ada".equals(m.getBody())
                        && "Test subject".equals(m.getSubject())));
    verify(sentLog, never()).load();
    verify(sentLog, never()).record(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void sendTestEmailReturnsFalseOnSendFailure() throws Exception {
    Path body = tempDir.resolve("body.txt");
    Files.writeString(body, "Hi {{name}}", StandardCharsets.UTF_8);

    EmailSender emailSender = mock(EmailSender.class);
    doThrow(new RuntimeException("smtp failed")).when(emailSender).send(org.mockito.ArgumentMatchers.any(EmailMessage.class));

    MailAppProperties properties = new MailAppProperties();
    properties.setSubject("Test subject");
    SentAddressLog sentLog = mock(SentAddressLog.class);

    MailBody mailBody = new MailBody(emailSender, properties, sentLog);
    assertFalse(mailBody.sendTestEmail(body.toString(), new Contact("test@example.com", "Ada")));
    verify(sentLog, never()).record(org.mockito.ArgumentMatchers.anyString());
  }
}
