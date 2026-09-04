package com.mailSender;

import com.mailSender.campaign.EmailComposer;
import com.mailSender.campaign.EmailMessage;
import com.mailSender.excel.Contact;
import com.mailSender.smtp.EmailSender;
import com.mailSender.template.EmailTemplate;
import com.mailSender.template.TemplateRenderer;
import com.mailSender.template.TemplateValidator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Campaign send loop: skip already-sent addresses, compose an {@link EmailMessage}, then send it.
 * Generation ({@link EmailComposer}) is separate from transport ({@link EmailSender}).
 */
@Component
public class MailBody {

  private static final Logger log = LoggerFactory.getLogger(MailBody.class);

  private final EmailSender emailSender;
  private final MailAppProperties mailAppProperties;
  private final SentAddressLog sentAddressLog;
  private final EmailComposer emailComposer;

  public MailBody(
      EmailSender emailSender,
      MailAppProperties mailAppProperties,
      SentAddressLog sentAddressLog) {
    this.emailSender = emailSender;
    this.mailAppProperties = mailAppProperties;
    this.sentAddressLog = sentAddressLog;
    this.emailComposer = new EmailComposer(mailAppProperties);
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
      EmailMessage message = emailComposer.compose(recipient, template);
      if (deliver(message, alreadySent)) {
        sent++;
      } else {
        failed++;
      }
      delayNextAttempt = shouldDelay();
    }
    log.info("Batch summary: sent={}, failed={}, skipped={}", sent, failed, skipped);
    if (failed > 0) {
      throw new IllegalStateException("Batch had " + failed + " send failure(s)");
    }
  }

  /**
   * Sends one rendered message to {@code recipient} (the test address). Does not walk the Excel
   * list or write the sent-address log. Returns {@code true} on success and {@code false} on send
   * failure.
   */
  public boolean sendTestEmail(String textFilePath, Contact recipient) {
    String template = readFileContent(textFilePath);
    TemplateValidator.validate(
        mailAppProperties.getSubject(), template, placeholderKeysFrom(List.of(recipient)));
    EmailMessage message = emailComposer.compose(recipient, template);
    try {
      emailSender.send(message);
      log.info("Test email sent successfully to {}", recipient.getEmail());
      return true;
    } catch (RuntimeException e) {
      log.warn("Test email failed to {}: {}", recipient.getEmail(), e.getMessage());
      return false;
    }
  }

  /** Hands a composed message to {@link EmailSender}; does not render the template. */
  private boolean deliver(EmailMessage message, Set<String> alreadySent) {
    String email = message.getTo();
    try {
      emailSender.send(message);
      alreadySent.add(SentAddressLog.normalize(email));
      if (!mailAppProperties.isDryRun()) {
        try {
          sentAddressLog.record(email);
        } catch (RuntimeException logError) {
          log.warn(
              "Sent log write failed after SMTP success for {}: {}", email, logError.getMessage());
        }
      }
      return true;
    } catch (RuntimeException e) {
      log.warn("Failed to send to {}: {}", email, e.getMessage());
      return false;
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
