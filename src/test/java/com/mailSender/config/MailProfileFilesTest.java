package com.mailSender.config;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MailProfileFilesTest {

  @Test
  void committedProfileFilesDoNotContainLiteralSecrets() throws Exception {
    String development =
        Files.readString(
            Path.of("src/main/resources/application-development.properties"),
            StandardCharsets.UTF_8);
    String production =
        Files.readString(
            Path.of("src/main/resources/application-production.properties"), StandardCharsets.UTF_8);
    assertFalse(development.contains("spring.mail.password="));
    assertFalse(production.contains("spring.mail.password="));
    assertFalse(production.contains("MAIL_PASSWORD="));
    assertFalse(production.toLowerCase().contains("app-password"));
  }
}
