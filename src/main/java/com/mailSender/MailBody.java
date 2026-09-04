package com.mailSender;

import com.mailSender.excel.Contact;
import com.mailSender.template.EmailTemplate;
import com.mailSender.template.TemplateRenderer;
import com.mailSender.template.TemplateValidator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MailBody {

  private static final Logger log = LoggerFactory.getLogger(MailBody.class);

  private final EmailService emailService;
  private final MailAppProperties mailAppProperties;
  private final SentAddressLog sentAddressLog;

  public MailBody(
      EmailService emailService,
      MailAppProperties mailAppProperties,
      SentAddressLog sentAddressLog) {
    this.emailService = emailService;
    this.mailAppProperties = mailAppProperties;
    this.sentAddressLog = sentAddressLog;
  }

  public static String readFileContent(String filePath) {
    return EmailTemplate.load(filePath).getBody();
  }

  public static String personalize(String template, Contact recipient) {
    return TemplateRenderer.render(template, recipient);
  }

  public void sendPersonalizedEmails(String textFilePath, List<Contact> recipients) {
    sendPersonalizedEmails(textFilePath, recipients, placeholderKeysFrom(recipients));
  }

  public void sendPersonalizedEmails(
      String textFilePath, List<Contact> recipients, Set<String> placeholderKeys) {
    String template = readFileContent(textFilePath);
    TemplateValidator.validate(mailAppProperties.getSubject(), template, placeholderKeys);
    if (recipients == null || recipients.isEmpty()) {
      log.warn(
          "No recipients to send: Excel had no usable rows (header-only or all rows skipped).");
      log.info("Batch summary: sent=0, failed=0, skipped=0");
      return;
    }
    Set<String> alreadySent = new LinkedHashSet<>(sentAddressLog.load());
    int sent = 0;
    int failed = 0;
    int skipped = 0;
    boolean delayNextAttempt = false;
    for (Contact recipient : recipients) {
      String email = recipient.getEmail();
      if (alreadySent.contains(SentAddressLog.normalize(email))) {
        skipped++;
        log.warn("Skipping already sent: {}", email);
        continue;
      }
      if (delayNextAttempt) {
        sleepBetweenSends();
      }
      String emailBody = personalize(template, recipient);
      try {
        emailService.sendEmail(email, emailBody);
        sent++;
        String normalized = SentAddressLog.normalize(email);
        alreadySent.add(normalized);
        if (!mailAppProperties.isDryRun()) {
          try {
            sentAddressLog.record(email);
          } catch (RuntimeException logError) {
            log.warn(
                "Sent log write failed after SMTP success for {}: {}",
                email,
                logError.getMessage());
          }
        }
      } catch (RuntimeException e) {
        failed++;
        log.warn("Failed to send to {}: {}", email, e.getMessage());
      }
      delayNextAttempt = shouldDelay();
    }
    log.info("Batch summary: sent={}, failed={}, skipped={}", sent, failed, skipped);
    if (failed > 0) {
      throw new IllegalStateException("Batch had " + failed + " send failure(s)");
    }
  }

  private static Set<String> placeholderKeysFrom(List<Contact> recipients) {
    Set<String> keys = new LinkedHashSet<>();
    keys.add("email");
    keys.add("name");
    if (recipients == null) {
      return keys;
    }
    for (Contact contact : recipients) {
      keys.addAll(contact.getPlaceholders().keySet());
    }
    return keys;
  }

  private boolean shouldDelay() {
    return !mailAppProperties.isDryRun() && mailAppProperties.getSendDelayMs() > 0;
  }

  private void sleepBetweenSends() {
    try {
      Thread.sleep(mailAppProperties.getSendDelayMs());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while delaying between sends", e);
    }
  }
}
