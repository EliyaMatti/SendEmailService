package com.mailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mailSender.config.SmtpConfiguration;
import com.mailSender.excel.Contact;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** CLI runner: batch skip, SMTP preflight, test-send does not send the Excel list. */
class BatchMailRunnerTest {

  @Test
  void realSendRequiresSmtpCredentials() {
    MailAppProperties properties = new MailAppProperties();
    properties.setBatchEnabled(true);
    properties.setDryRun(false);
    properties.setExcelFilePath("excel.xlsx");
    properties.setBodyFilePath("body.txt");
    properties.setFrom("");
    BatchMailRunner runner = new BatchMailRunner(properties, mock(MailBody.class), smtp("", "", ""));
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
        new BatchMailRunner(
            properties,
            mock(MailBody.class),
            smtp("user@example.com", "secret", "from@example.com"));
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> runner.run());
    assertTrue(ex.getMessage().contains("mail.sent-log-path"));
  }

  @Test
  void disabledBatchDoesNotRequireSmtpCredentials() {
    MailAppProperties properties = new MailAppProperties();
    properties.setBatchEnabled(false);
    BatchMailRunner runner = new BatchMailRunner(properties, mock(MailBody.class), smtp("", "", ""));
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
        new BatchMailRunner(
            properties, mailBody, smtp("user@example.com", "secret", "from@example.com"));
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> runner.run());
    assertTrue(ex.getMessage().contains("Cannot read attachment"));
    verify(mailBody, never()).sendPersonalizedEmails(anyString(), anyList(), anySet());
  }

  @Test
  void testSendDoesNotSendExcelListEvenWhenBatchEnabled() {
    MailAppProperties properties = new MailAppProperties();
    properties.setBatchEnabled(true);
    properties.setTestSendEnabled(true);
    properties.setTestSendTo("me@example.com");
    properties.setBodyFilePath("body.txt");
    properties.setExcelFilePath("");
    properties.setDryRun(true);
    MailBody mailBody = mock(MailBody.class);
    when(mailBody.sendTestEmail(anyString(), org.mockito.ArgumentMatchers.any(Contact.class)))
        .thenReturn(true);
    BatchMailRunner runner = new BatchMailRunner(properties, mailBody, smtp("", "", ""));
    assertDoesNotThrow(() -> runner.run());
    verify(mailBody).sendTestEmail(anyString(), org.mockito.ArgumentMatchers.any(Contact.class));
    verify(mailBody, never()).sendPersonalizedEmails(anyString(), anyList(), anySet());
  }

  @Test
  void testSendRequiresDestinationAddress() {
    MailAppProperties properties = new MailAppProperties();
    properties.setTestSendEnabled(true);
    properties.setBodyFilePath("body.txt");
    properties.setTestSendTo("");
    BatchMailRunner runner = new BatchMailRunner(properties, mock(MailBody.class), smtp("", "", ""));
    IllegalStateException ex = assertThrows(IllegalStateException.class, () -> runner.run());
    assertTrue(ex.getMessage().contains("mail.test-send-to"));
  }

  @Test
  void copyWithToUsesTestAddressAndKeepsPlaceholders() {
    Contact sample = new Contact("row@example.com", "Ada", Map.of("company", "Acme"));
    Contact test = BatchMailRunner.copyWithTo(sample, "me@example.com");
    assertEquals("me@example.com", test.getEmail());
    assertEquals("Ada", test.getName());
    assertEquals("Acme", test.getPlaceholders().get("company"));
  }

  private static SmtpConfiguration smtp(String username, String password, String fromEmail) {
    return new SmtpConfiguration("smtp.gmail.com", 587, username, password, fromEmail, "", true);
  }
}
