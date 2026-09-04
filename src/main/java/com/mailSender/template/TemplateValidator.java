package com.mailSender.template;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;

public final class TemplateValidator {

  private TemplateValidator() {}

  public static void validate(String subject, String body, Set<String> availablePlaceholderKeys) {
    List<String> errors = new ArrayList<>();
    if (subject == null || subject.isBlank()) {
      errors.add("Subject is empty.");
    }
    if (body == null || body.isBlank()) {
      errors.add("Body is empty.");
    }
    Set<String> keys = normalizeKeys(availablePlaceholderKeys);
    if (!keys.contains("email") || !keys.contains("name")) {
      errors.add("Imported data is missing required fields: email and name.");
    }
    if (body != null && !body.isEmpty()) {
      errors.addAll(placeholderErrors(body, keys));
    }
    if (!errors.isEmpty()) {
      throw new IllegalStateException(
          "Template validation failed:\n" + String.join("\n", errors));
    }
  }

  private static Set<String> normalizeKeys(Set<String> availablePlaceholderKeys) {
    Set<String> keys = new LinkedHashSet<>();
    if (availablePlaceholderKeys == null) {
      return keys;
    }
    for (String key : availablePlaceholderKeys) {
      if (key != null && !key.isBlank()) {
        keys.add(key.toLowerCase(Locale.ROOT));
      }
    }
    return keys;
  }

  private static List<String> placeholderErrors(String body, Set<String> keys) {
    List<String> errors = new ArrayList<>();
    int i = 0;
    while (i < body.length()) {
      int open = body.indexOf("{{", i);
      int strayClose = body.indexOf("}}", i);
      if (open < 0) {
        if (strayClose >= 0) {
          errors.add("Invalid placeholder syntax near '}}'.");
        }
        break;
      }
      if (strayClose >= 0 && strayClose < open) {
        errors.add("Invalid placeholder syntax near '}}'.");
        i = strayClose + 2;
        continue;
      }
      Matcher matcher = TemplateRenderer.PLACEHOLDER.matcher(body);
      matcher.region(open, body.length());
      if (!matcher.lookingAt()) {
        errors.add("Invalid placeholder syntax starting at '{{'.");
        i = open + 2;
        continue;
      }
      String originalKey = matcher.group(1);
      String key = originalKey.toLowerCase(Locale.ROOT);
      if (!keys.contains(key)) {
        errors.add(
            "Placeholder {{" + originalKey + "}} does not exist in the imported data.");
      }
      i = matcher.end();
    }
    return errors;
  }
}
