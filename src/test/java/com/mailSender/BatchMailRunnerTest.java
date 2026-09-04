package com.mailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

class BatchMailRunnerTest {

  @Test
  void realSendRequiresSmtpCredentials() {
    MailAppProperties properties = new MailAppProperties();
    properties.setBatchEnabled(true);
    properties.setDryRun(false);
    properties.setExcelFilePath("excel.xlsx");
    properties.setBodyFilePath("body.txt");
    properties.setFrom("");
    BatchMailRunner runner = new BatchMailRunner(properties, mock(MailBody.class), "", "");
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> runner.run());
    assertTrue(ex.getMessage().contains("Real send requires"));
  }

  @Test
  void realSendRequiresSentLogPath() {
    MailAppProperties properties = new MailAppProperties();
    properties.setBatchEnabled(true);
    properties.setDryRun(false);
    properties.setExcelFilePath("excel.xlsx");
    properties.setBodyFilePath("body.txt");
    properties.setFrom("from@example.com");
    properties.setSentLogPath("");
    BatchMailRunner runner =
        new BatchMailRunner(properties, mock(MailBody.class), "user@example.com", "secret");
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> runner.run());
    assertTrue(ex.getMessage().contains("mail.sent-log-path"));
  }

  @Test
  void disabledBatchDoesNotRequireSmtpCredentials() {
    MailAppProperties properties = new MailAppProperties();
    properties.setBatchEnabled(false);
    BatchMailRunner runner = new BatchMailRunner(properties, mock(MailBody.class), "", "");
    assertDoesNotThrow(() -> runner.run());
  }

  @Test
  void missingAttachmentFailsBeforeSendWhenDryRunOff() {
    MailAppProperties properties = new MailAppProperties();
    properties.setBatchEnabled(true);
    properties.setDryRun(false);
    properties.setExcelFilePath("excel.xlsx");
    properties.setBodyFilePath("body.txt");
    properties.setFrom("from@example.com");
    properties.setSentLogPath("sent-addresses.txt");
    properties.setAttachmentPath("missing-attachment.pdf");
    MailBody mailBody = mock(MailBody.class);
    BatchMailRunner runner =
        new BatchMailRunner(properties, mailBody, "user@example.com", "secret");
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> runner.run());
    assertTrue(ex.getMessage().contains("Cannot read attachment"));
    verify(mailBody, never()).sendPersonalizedEmails(anyString(), anyList(), anySet());
  }
}
