package com.mailSender;

import java.util.LinkedHashMap;
import java.util.Map;

public class EmailRecipient {
  private final String email;
  private final String name;
  private final Map<String, String> placeholders;

  public EmailRecipient(String email, String name) {
    this.email = email;
    this.name = name;
    this.placeholders = new LinkedHashMap<>();
    if (email != null) {
      this.placeholders.put("email", email);
    }
    if (name != null) {
      this.placeholders.put("name", name);
    }
  }

  public String getEmail() {
    return email;
  }

  public String getName() {
    return name;
  }

  public Map<String, String> getPlaceholders() {
    return placeholders;
  }
}
