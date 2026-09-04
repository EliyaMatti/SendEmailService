package com.mailSender;

import com.mailSender.config.SmtpConfiguration;
import com.mailSender.excel.Contact;
import com.mailSender.excel.ExcelReadResult;
import com.mailSender.excel.ExcelReader;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Startup CLI: optional one-address test send, or the Excel batch. Never sends the full list when
 * test-send is enabled.
 */
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
    if (mailAppProperties.isTestSendEnabled()) {
      runTestSend();
      return;
    }
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
      requireReadableAttachmentIfSet();
    }
    ExcelReadResult excel = ExcelReader.read(excelFilePath);
    mailBody.sendPersonalizedEmails(
        bodyFilePath, excel.getContacts(), excel.getPlaceholderKeys());
  }

  private void runTestSend() {
    String bodyFilePath = mailAppProperties.getBodyFilePath();
    String testTo = mailAppProperties.getTestSendTo();
    if (isBlank(bodyFilePath)) {
      throw new IllegalStateException(
          "Test send requires mail.body-file-path (MAIL_BODY_FILE_PATH)");
    }
    if (isBlank(testTo)) {
      throw new IllegalStateException("Test send requires mail.test-send-to (MAIL_TEST_SEND_TO)");
    }
    if (mailAppProperties.isDryRun()) {
      log.info(
          "Test send dry-run: one message to {}; SMTP is skipped (set MAIL_DRY_RUN=false to send).",
          testTo);
    } else {
      requireSmtpConfig();
      requireReadableAttachmentIfSet();
    }
    Contact recipient = testSendRecipient(testTo);
    boolean success = mailBody.sendTestEmail(bodyFilePath, recipient);
    if (!success) {
      throw new IllegalStateException("Test email failed to " + testTo);
    }
  }

  private Contact testSendRecipient(String testTo) {
    String excelFilePath = mailAppProperties.getExcelFilePath();
    if (isBlank(excelFilePath)) {
      return new Contact(testTo, "Test");
    }
    ExcelReadResult excel = ExcelReader.read(excelFilePath);
    if (excel.getContacts().isEmpty()) {
      throw new IllegalStateException(
          "Test send needs at least one valid Excel row to fill placeholders");
    }
    return copyWithTo(excel.getContacts().get(0), testTo);
  }

  static Contact copyWithTo(Contact sample, String testTo) {
    Map<String, String> extras = new LinkedHashMap<>(sample.getPlaceholders());
    extras.remove("email");
    extras.remove("name");
    return new Contact(testTo, sample.getName(), extras);
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

  private void requireReadableAttachmentIfSet() {
    String attachmentPath = mailAppProperties.getAttachmentPath();
    if (attachmentPath != null && !attachmentPath.isBlank()) {
      File attachment = new File(attachmentPath);
      if (!attachment.isFile() || !attachment.canRead()) {
        throw new IllegalStateException("Cannot read attachment: " + attachment.getAbsolutePath());
      }
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
