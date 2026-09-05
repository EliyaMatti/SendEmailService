package com.mailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mailSender.campaign.EmailMessage;
import com.mailSender.config.MailAppProperties;
import com.mailSender.config.SmtpConfiguration;
import com.mailSender.smtp.EmailSender;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

/**
 * Regression: original Excel list → compose from the body file → send. Mock {@link EmailSender};
 * no live SMTP. Distinct from test-send (M1-025), which mails one configured address.
 */
class OriginalExcelSmtpRegressionTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("Excel → read addresses → compose → send (batch)")
  void batchStillSendsOneMessagePerExcelContact() throws Exception {
    Path excel = writeRecipientsXlsx();
    Path body = tempDir.resolve("body.txt");
    Files.writeString(body, "Hello {{Name}}", StandardCharsets.UTF_8);

    MailAppProperties properties = new MailAppProperties();
    properties.setBatchEnabled(true);
    properties.setTestSendEnabled(false);
    properties.setExcelFilePath(excel.toString());
    properties.setBodyFilePath(body.toString());
    properties.setSubject("Fixed subject");
    properties.setFrom("from@example.com");
    properties.setDryRun(true);
    properties.setSendDelayMs(0);

    EmailSender emailSender = mock(EmailSender.class);
    SentAddressLog sentLog = mock(SentAddressLog.class);
    when(sentLog.load()).thenReturn(Set.of());
    MailBody mailBody = new MailBody(emailSender, properties, sentLog);
    BatchMailRunner runner = new BatchMailRunner(properties, mailBody, smtpUnused());

    runner.run();

    ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
    verify(emailSender, times(2)).send(captor.capture());
    List<EmailMessage> messages = captor.getAllValues();
    assertEquals("ada@example.com", messages.get(0).getTo());
    assertEquals("Hello Ada", messages.get(0).getBody());
    assertEquals("Fixed subject", messages.get(0).getSubject());
    assertEquals("from@example.com", messages.get(0).getFrom());
    assertEquals("bob@example.com", messages.get(1).getTo());
    assertEquals("Hello Bob", messages.get(1).getBody());
    verify(sentLog, never()).record(org.mockito.ArgumentMatchers.anyString());
  }

  private Path writeRecipientsXlsx() throws Exception {
    Path excel = tempDir.resolve("recipients.xlsx");
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet();
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("Email");
      header.createCell(1).setCellValue("Name");
      Row ada = sheet.createRow(1);
      ada.createCell(0).setCellValue("ada@example.com");
      ada.createCell(1).setCellValue("Ada");
      Row bob = sheet.createRow(2);
      bob.createCell(0).setCellValue("bob@example.com");
      bob.createCell(1).setCellValue("Bob");
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
