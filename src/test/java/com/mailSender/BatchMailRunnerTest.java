package com.mailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

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
  void disabledBatchDoesNotRequireSmtpCredentials() {
    MailAppProperties properties = new MailAppProperties();
    properties.setBatchEnabled(false);
    BatchMailRunner runner = new BatchMailRunner(properties, mock(MailBody.class), "", "");
    assertDoesNotThrow(() -> runner.run());
  }
}
