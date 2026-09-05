package com.mailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mailSender.campaign.EmailMessage;
import com.mailSender.config.MailAppProperties;
import com.mailSender.config.SmtpConfiguration;
import com.mailSender.smtp.EmailSender;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

/**
 * Local end-to-end: Excel → read/validate → Contact → render → EmailMessage → test-send. Transport
 * is a mock {@link EmailSender}; no live SMTP.
 */
class EndToEndLocalWorkflowTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("Excel → Contact → template → EmailMessage → test send")
  void testSendUsesFirstExcelRowPlaceholdersAndDoesNotCallSmtp() throws Exception {
    Path excel = writeRecipientsXlsx();
    Path body = tempDir.resolve("body.txt");
    Files.writeString(
        body,
        "Hi {{Name}}, you work at {{Company}}. This goes to {{Email}}.",
        StandardCharsets.UTF_8);

    MailAppProperties properties = new MailAppProperties();
    properties.setTestSendEnabled(true);
    properties.setTestSendTo("tester@example.com");
    properties.setExcelFilePath(excel.toString());
    properties.setBodyFilePath(body.toString());
    properties.setSubject("Welcome");
    properties.setFrom("from@example.com");
    properties.setDryRun(true);
    properties.setBatchEnabled(true);

    EmailSender emailSender = mock(EmailSender.class);
    SentAddressLog sentLog = mock(SentAddressLog.class);
    MailBody mailBody = new MailBody(emailSender, properties, sentLog);
    BatchMailRunner runner =
        new BatchMailRunner(properties, mailBody, smtpUnused());

    runner.run();

    ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
    verify(emailSender, times(1)).send(captor.capture());
    EmailMessage message = captor.getValue();
    assertEquals("tester@example.com", message.getTo());
    assertEquals("Welcome", message.getSubject());
    assertEquals("from@example.com", message.getFrom());
    assertEquals(
        "Hi Ada, you work at Acme. This goes to tester@example.com.", message.getBody());
    verify(sentLog, never()).record(org.mockito.ArgumentMatchers.anyString());
  }

  private Path writeRecipientsXlsx() throws Exception {
    Path excel = tempDir.resolve("recipients.xlsx");
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet();
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("Email");
      header.createCell(1).setCellValue("Name");
      header.createCell(2).setCellValue("Company");
      Row ada = sheet.createRow(1);
      ada.createCell(0).setCellValue("ada@example.com");
      ada.createCell(1).setCellValue("Ada");
      ada.createCell(2).setCellValue("Acme");
      Row bob = sheet.createRow(2);
      bob.createCell(0).setCellValue("bob@example.com");
      bob.createCell(1).setCellValue("Bob");
      bob.createCell(2).setCellValue("Beta");
      try (var out = Files.newOutputStream(excel)) {
        workbook.write(out);
      }
    }
    return excel;
  }

  private static SmtpConfiguration smtpUnused() {
    return new SmtpConfiguration("smtp.example.com", 587, "", "", "", "", true);
  }
}
