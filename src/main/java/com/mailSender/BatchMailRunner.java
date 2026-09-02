package com.mailSender;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class BatchMailRunner implements CommandLineRunner {

  private final MailAppProperties mailAppProperties;
  private final MailBody mailBody;

  public BatchMailRunner(MailAppProperties mailAppProperties, MailBody mailBody) {
    this.mailAppProperties = mailAppProperties;
    this.mailBody = mailBody;
  }

  @Override
  public void run(String... args) {
    if (!mailAppProperties.isBatchEnabled()) {
      return;
    }
    String excelFilePath = mailAppProperties.getExcelFilePath();
    String bodyFilePath = mailAppProperties.getBodyFilePath();
    if (excelFilePath == null
        || excelFilePath.isBlank()
        || bodyFilePath == null
        || bodyFilePath.isBlank()) {
      System.out.println(
          "Mail batch skipped: set mail.excel-file-path and mail.body-file-path (or MAIL_EXCEL_FILE_PATH / MAIL_BODY_FILE_PATH).");
      return;
    }
    List<EmailRecipient> recipients = ReadFromExcel.readEmailsAndNamesFromExcel(excelFilePath);
    mailBody.sendPersonalizedEmails(bodyFilePath, recipients);
  }
}
