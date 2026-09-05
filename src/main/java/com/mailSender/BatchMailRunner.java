package com.mailSender;

import com.mailSender.config.ApplicationProfiles;
import com.mailSender.config.MailAppProperties;
import com.mailSender.config.SmtpConfiguration;
import com.mailSender.config.SmtpConfigurationException;
import com.mailSender.excel.Contact;
import com.mailSender.excel.ExcelProcessingException;
import com.mailSender.excel.ExcelReadResult;
import com.mailSender.excel.ExcelReader;
import com.mailSender.smtp.EmailSendingException;
import com.mailSender.template.TemplateValidationException;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Startup CLI: optional one-address test send, or the Excel batch. Never sends the full list when
 * test-send is enabled. Disabled on the {@code api} profile so HTTP startup does not send mail.
 */
@Component
@Profile("!" + ApplicationProfiles.API)
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
    try {
      runJob(args);
    } catch (ExcelProcessingException
        | TemplateValidationException
        | SmtpConfigurationException
        | EmailSendingException e) {
      throw e;
    } catch (RuntimeException e) {
      log.error("Unexpected error while running the mail job", e);
      throw new EmailSendingException(
          "Unable to complete the mail job. See the application log for technical details.", e);
    }
  }

  private void runJob(String... args) {
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
      logSmtpConnectionResult(true);
    } else {
      requireSmtpConfig();
      requireSentLogPath();
      requireReadableAttachmentIfSet();
      logSmtpConnectionResult(false);
    }
    ExcelReadResult excel = ExcelReader.read(excelFilePath);
    mailBody.sendPersonalizedEmails(
        bodyFilePath, excel.getContacts(), excel.getPlaceholderKeys());
  }

  private void runTestSend() {
    String bodyFilePath = mailAppProperties.getBodyFilePath();
    String testTo = mailAppProperties.getTestSendTo();
    if (isBlank(bodyFilePath)) {
      throw new SmtpConfigurationException(
          "Unable to send a test email because mail.body-file-path is not set.");
    }
    if (isBlank(testTo)) {
      throw new SmtpConfigurationException(
          "Unable to send a test email because mail.test-send-to is not set.");
    }
    if (mailAppProperties.isDryRun()) {
      log.info(
          "Test send dry-run: one message to {}; SMTP is skipped (set MAIL_DRY_RUN=false to send).",
          testTo);
      logSmtpConnectionResult(true);
    } else {
      requireSmtpConfig();
      requireReadableAttachmentIfSet();
      logSmtpConnectionResult(false);
    }
    Contact recipient = testSendRecipient(testTo);
    boolean success = mailBody.sendTestEmail(bodyFilePath, recipient);
    if (!success) {
      throw new EmailSendingException(
          "Unable to send a test email to " + testTo + ". See the log for the reason.");
    }
  }

  private Contact testSendRecipient(String testTo) {
    String excelFilePath = mailAppProperties.getExcelFilePath();
    if (isBlank(excelFilePath)) {
      return new Contact(testTo, "Test");
    }
    ExcelReadResult excel = ExcelReader.read(excelFilePath);
    if (excel.getContacts().isEmpty()) {
      throw new ExcelProcessingException(
          "Unable to send a test email because the Excel file has no valid contacts to fill placeholders.");
    }
    return contactForTestSend(excel.getContacts().get(0), testTo);
  }

  static Contact contactForTestSend(Contact sample, String testTo) {
    Map<String, String> extras = new LinkedHashMap<>(sample.getPlaceholders());
    extras.remove("email");
    extras.remove("name");
    return new Contact(testTo, sample.getName(), extras);
  }

  private void logSmtpConnectionResult(boolean dryRun) {
    if (dryRun) {
      log.info("SMTP connection skipped (dry-run)");
      return;
    }
    log.info(
        "SMTP connection ready: host={} port={} tlsEnabled={} authEnabled={}",
        smtpConfiguration.getHost(),
        smtpConfiguration.getPort(),
        smtpConfiguration.isTlsEnabled(),
        smtpConfiguration.isAuthEnabled());
  }

  private void requireSmtpConfig() {
    if (!smtpConfiguration.isReadyForSend()) {
      throw new SmtpConfigurationException(
          "Unable to send mail because SMTP username, password, and from address are required.");
    }
  }

  private void requireSentLogPath() {
    if (isBlank(mailAppProperties.getSentLogPath())) {
      throw new SmtpConfigurationException(
          "Unable to send mail because mail.sent-log-path is not set (needed to skip already-sent addresses).");
    }
  }

  private void requireReadableAttachmentIfSet() {
    String attachmentPath = mailAppProperties.getAttachmentPath();
    if (attachmentPath != null && !attachmentPath.isBlank()) {
      File attachment = new File(attachmentPath);
      if (!attachment.isFile() || !attachment.canRead()) {
        throw new SmtpConfigurationException(
            "Unable to send mail because the attachment file could not be read: "
                + attachment.getAbsolutePath());
      }
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
