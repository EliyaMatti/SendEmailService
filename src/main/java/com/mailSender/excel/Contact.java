package com.mailSender.excel;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Contact {
  private final String email;
  private final String name;
  private final Map<String, String> placeholders;

  public Contact(String email, String name) {
    this(email, name, Map.of());
  }

  public Contact(String email, String name, Map<String, String> extraPlaceholders) {
    this.email = email;
    this.name = name;
    Map<String, String> values = new LinkedHashMap<>();
    if (email != null) {
      values.put("email", email);
    }
    if (name != null) {
      values.put("name", name);
    }
    if (extraPlaceholders != null) {
      extraPlaceholders.forEach(
          (key, value) -> {
            if (key != null && !key.isBlank() && !"email".equals(key) && !"name".equals(key)) {
              values.put(key, value == null ? "" : value);
            }
          });
    }
    this.placeholders = Collections.unmodifiableMap(values);
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
