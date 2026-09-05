package com.mailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mailSender.config.MailAppProperties;
import com.mailSender.config.SmtpConfiguration;
import com.mailSender.config.SmtpConfigurationException;
import com.mailSender.excel.Contact;
import com.mailSender.smtp.EmailSendingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

/** CLI runner: batch skip, SMTP preflight, test-send does not send the Excel list. */
class BatchMailRunnerTest {

  @TempDir Path tempDir;

  @Test
  void realSendRequiresSmtpCredentials() {
    MailAppProperties properties = new MailAppProperties();
    properties.setBatchEnabled(true);
    properties.setDryRun(false);
    properties.setExcelFilePath("excel.xlsx");
    properties.setBodyFilePath("body.txt");
    properties.setFrom("");
    BatchMailRunner runner = new BatchMailRunner(properties, mock(MailBody.class), smtp("", "", ""));
    SmtpConfigurationException ex = assertThrows(SmtpConfigurationException.class, () -> runner.run());
    assertTrue(ex.getMessage().contains("Unable to send mail because SMTP username"));
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
    SmtpConfigurationException ex = assertThrows(SmtpConfigurationException.class, () -> runner.run());
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
    SmtpConfigurationException ex = assertThrows(SmtpConfigurationException.class, () -> runner.run());
    assertTrue(ex.getMessage().contains("attachment file could not be read"));
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
    SmtpConfigurationException ex = assertThrows(SmtpConfigurationException.class, () -> runner.run());
    assertTrue(ex.getMessage().contains("mail.test-send-to"));
  }

  @Test
  void contactForTestSendUsesTestAddressAndKeepsPlaceholders() {
    Contact sample = new Contact("row@example.com", "Ada", Map.of("company", "Acme"));
    Contact test = BatchMailRunner.contactForTestSend(sample, "me@example.com");
    assertEquals("me@example.com", test.getEmail());
    assertEquals("Ada", test.getName());
    assertEquals("Acme", test.getPlaceholders().get("company"));
  }

  @Test
  void unexpectedNullPointerIsLoggedNotShownToOperator() throws Exception {
    Path excel = tempDir.resolve("ok.xlsx");
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet();
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("email");
      header.createCell(1).setCellValue("name");
      Row row = sheet.createRow(1);
      row.createCell(0).setCellValue("a@example.com");
      row.createCell(1).setCellValue("Ada");
      try (var out = Files.newOutputStream(excel)) {
        workbook.write(out);
      }
    }
    Path body = tempDir.resolve("body.txt");
    Files.writeString(body, "Hi");
    MailAppProperties properties = new MailAppProperties();
    properties.setBatchEnabled(true);
    properties.setDryRun(true);
    properties.setExcelFilePath(excel.toString());
    properties.setBodyFilePath(body.toString());
    MailBody mailBody = mock(MailBody.class);
    doThrow(new NullPointerException("internal"))
        .when(mailBody)
        .sendPersonalizedEmails(anyString(), anyList(), anySet());
    EmailSendingException ex =
        assertThrows(
            EmailSendingException.class,
            () -> new BatchMailRunner(properties, mailBody, smtp("", "", "")).run());
    assertTrue(ex.getMessage().contains("Unable to complete the mail job"));
    assertFalse(ex.getMessage().contains("NullPointerException"));
    assertTrue(ex.getCause() instanceof NullPointerException);
  }

  @Test
  void realSendLogsSmtpConnectionWithoutPassword() {
    MailAppProperties properties = new MailAppProperties();
    properties.setBatchEnabled(true);
    properties.setDryRun(false);
    properties.setExcelFilePath("excel.xlsx");
    properties.setBodyFilePath("body.txt");
    properties.setFrom("from@example.com");
    properties.setSentLogPath("sent-addresses.txt");
    MailBody mailBody = mock(MailBody.class);
    Logger logger = (Logger) LoggerFactory.getLogger(BatchMailRunner.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      assertThrows(
          RuntimeException.class,
          () ->
              new BatchMailRunner(
                      properties,
                      mailBody,
                      smtp("user@example.com", "super-secret-app-password", "from@example.com"))
                  .run());
      String joined =
          String.join("\n", appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList());
      assertTrue(joined.contains("SMTP connection ready:"));
      assertTrue(joined.contains("host=smtp.gmail.com"));
      assertTrue(
          appender.list.stream()
              .noneMatch(e -> e.getFormattedMessage().contains("super-secret-app-password")));
    } finally {
      logger.detachAppender(appender);
    }
  }

  private static SmtpConfiguration smtp(String username, String password, String fromEmail) {
    return new SmtpConfiguration("smtp.gmail.com", 587, username, password, fromEmail, "", true);
  }
}
