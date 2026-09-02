package com.mailSender;

import java.io.File;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class BatchMailRunner implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(BatchMailRunner.class);

  private final MailAppProperties mailAppProperties;
  private final MailBody mailBody;
  private final String smtpUsername;
  private final String smtpPassword;

  public BatchMailRunner(
      MailAppProperties mailAppProperties,
      MailBody mailBody,
      @Value("${spring.mail.username:}") String smtpUsername,
      @Value("${spring.mail.password:}") String smtpPassword) {
    this.mailAppProperties = mailAppProperties;
    this.mailBody = mailBody;
    this.smtpUsername = smtpUsername;
    this.smtpPassword = smtpPassword;
  }

  @Override
  public void run(String... args) {
    if (!mailAppProperties.isBatchEnabled()) {
      log.info(
          "Mail batch skipped: mail.batch-enabled is false (set MAIL_BATCH_ENABLED=true to run).");
      return;
    }
    String excelFilePath = mailAppProperties.getExcelFilePath();
    String bodyFilePath = mailAppProperties.getBodyFilePath();
    if (excelFilePath == null
        || excelFilePath.isBlank()
        || bodyFilePath == null
        || bodyFilePath.isBlank()) {
      log.info(
          "Mail batch skipped: set mail.excel-file-path and mail.body-file-path (or MAIL_EXCEL_FILE_PATH / MAIL_BODY_FILE_PATH).");
      return;
    }
    if (mailAppProperties.isDryRun()) {
      log.info(
          "Mail batch dry-run: printing To and body; SMTP is skipped (set MAIL_DRY_RUN=false to send).");
    } else {
      requireSmtpConfig();
      String attachmentPath = mailAppProperties.getAttachmentPath();
      if (attachmentPath != null && !attachmentPath.isBlank()) {
        File attachment = new File(attachmentPath);
        if (!attachment.isFile() || !attachment.canRead()) {
          throw new IllegalStateException(
              "Cannot read attachment: " + attachment.getAbsolutePath());
        }
      }
    }
    List<EmailRecipient> recipients = ReadFromExcel.readEmailsAndNamesFromExcel(excelFilePath);
    mailBody.sendPersonalizedEmails(bodyFilePath, recipients);
  }

  private void requireSmtpConfig() {
    if (isBlank(smtpUsername) || isBlank(smtpPassword) || isBlank(mailAppProperties.getFrom())) {
      throw new IllegalStateException(
          "Real send requires spring.mail.username, spring.mail.password, and mail.from");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
