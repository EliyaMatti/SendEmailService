package com.mailSender.template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EmailTemplate {

  private final String body;

  public EmailTemplate(String body) {
    this.body = body;
  }

  public static EmailTemplate load(String filePath) {
    try {
      return new EmailTemplate(Files.readString(Path.of(filePath), StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read body file: " + filePath, e);
    }
  }

  public String getBody() {
    return body;
  }
}
