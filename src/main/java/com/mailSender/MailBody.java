package com.mailSender;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class MailBody {

  private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)\\}\\}");

  private final EmailService emailService;

  public MailBody(EmailService emailService) {
    this.emailService = emailService;
  }

  public static String readFileContent(String filePath) {
    StringBuilder content = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
      String line;
      while ((line = br.readLine()) != null) {
        content.append(line).append("\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    return content.toString();
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
    if (template == null) {
      return;
    }

    for (EmailRecipient recipient : recipients) {
      String emailBody = personalize(template, recipient);
      emailService.sendEmail(recipient.getEmail(), emailBody);
    }
  }
}
