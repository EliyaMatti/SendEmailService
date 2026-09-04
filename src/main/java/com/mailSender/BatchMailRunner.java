package com.mailSender;

import com.mailSender.config.SmtpConfiguration;
import com.mailSender.excel.ExcelReadResult;
import com.mailSender.excel.ExcelReader;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class BatchMailRunner implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(BatchMailRunner.class);

  private final MailAppProperties mailAppProperties;
  private final MailBody mailBody;
  private final SmtpConfiguration smtpConfiguration;

  public BatchMailRunner(
      MailAppProperties mailAppProperties, MailBody mailBody, SmtpConfiguration smtpConfiguration) {
    this.mailAppProperties = mailAppProperties;
    this.mailBody = mailBody;
    this.smtpConfiguration = smtpConfiguration;
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
      requireSentLogPath();
      String attachmentPath = mailAppProperties.getAttachmentPath();
      if (attachmentPath != null && !attachmentPath.isBlank()) {
        File attachment = new File(attachmentPath);
        if (!attachment.isFile() || !attachment.canRead()) {
          throw new IllegalStateException(
              "Cannot read attachment: " + attachment.getAbsolutePath());
        }
      }
    }
    ExcelReadResult excel = ExcelReader.read(excelFilePath);
    mailBody.sendPersonalizedEmails(
        bodyFilePath, excel.getContacts(), excel.getPlaceholderKeys());
  }

  private void requireSmtpConfig() {
    if (!smtpConfiguration.isReadyForSend()) {
      throw new IllegalStateException(
          "Real send requires spring.mail.username, spring.mail.password, and mail.from");
    }
  }

  private void requireSentLogPath() {
    if (isBlank(mailAppProperties.getSentLogPath())) {
      throw new IllegalStateException(
          "Real send requires mail.sent-log-path (MAIL_SENT_LOG_PATH) so addresses are not mailed twice");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
