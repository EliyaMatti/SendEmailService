package com.mailSender.template;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EmailTemplate {

  private static final Logger log = LoggerFactory.getLogger(EmailTemplate.class);

  private final String body;

  public EmailTemplate(String body) {
    this.body = body;
  }

  public static EmailTemplate load(String filePath) {
    try {
      return new EmailTemplate(Files.readString(Path.of(filePath), StandardCharsets.UTF_8));
    } catch (IOException e) {
      log.debug("Body file could not be read: {}", filePath, e);
      throw new TemplateValidationException(
          "Unable to read the email body file. Check mail.body-file-path and that the file exists.",
          e);
    }
  }

  public String getBody() {
    return body;
  }
}
