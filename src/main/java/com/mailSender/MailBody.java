package com.mailSender;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MailBody {

  private static final Logger log = LoggerFactory.getLogger(MailBody.class);
  private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)\\}\\}");

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
    try {
      return Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read body file: " + filePath, e);
    }
  }

  public static String personalize(String template, EmailRecipient recipient) {
    if (template == null || recipient == null) {
      return template;
    }
    Map<String, String> values = recipient.getPlaceholders();
    Matcher matcher = PLACEHOLDER.matcher(template);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      String key = matcher.group(1);
      String value = values.getOrDefault(key, "");
      matcher.appendReplacement(result, Matcher.quoteReplacement(value));
    }
    matcher.appendTail(result);
    return result.toString();
  }

  public void sendPersonalizedEmails(String textFilePath, List<EmailRecipient> recipients) {
    String template = readFileContent(textFilePath);
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
    for (EmailRecipient recipient : recipients) {
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
