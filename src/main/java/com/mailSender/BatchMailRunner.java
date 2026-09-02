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
      System.out.println(
          "Mail batch skipped: mail.batch-enabled is false (set MAIL_BATCH_ENABLED=true to run).");
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
    if (mailAppProperties.isDryRun()) {
      System.out.println(
          "Mail batch dry-run: printing To and body; SMTP is skipped (set MAIL_DRY_RUN=false to send).");
    }
    List<EmailRecipient> recipients = ReadFromExcel.readEmailsAndNamesFromExcel(excelFilePath);
    mailBody.sendPersonalizedEmails(bodyFilePath, recipients);
  }
}
