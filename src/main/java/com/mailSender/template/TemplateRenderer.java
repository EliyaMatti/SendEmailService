package com.mailSender.template;

import com.mailSender.excel.Contact;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TemplateRenderer {

  public static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{(\\w+)\\}\\}");

  private TemplateRenderer() {}

  public static String render(String template, Contact contact) {
    if (template == null || contact == null) {
      return template;
    }
    Map<String, String> values = contact.getPlaceholders();
    Matcher matcher = PLACEHOLDER.matcher(template);
    StringBuilder result = new StringBuilder();
    while (matcher.find()) {
      String key = matcher.group(1).toLowerCase(Locale.ROOT);
      String value = values.getOrDefault(key, "");
      matcher.appendReplacement(result, Matcher.quoteReplacement(value));
    }
    matcher.appendTail(result);
    return result.toString();
  }
}
