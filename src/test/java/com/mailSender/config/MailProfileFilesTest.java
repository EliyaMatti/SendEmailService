package com.mailSender.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  @Test
  void applicationPropertiesPasswordUsesEnvPlaceholdersOnly() throws Exception {
    String properties =
        Files.readString(Path.of("src/main/resources/application.properties"), StandardCharsets.UTF_8);
    assertTrue(properties.contains("spring.mail.password=${MAIL_PASSWORD:${SMTP_PASSWORD:}}"));
  }

  @Test
  void gitignoreAndExamplesKeepSecretsOutOfGit() throws Exception {
    String gitignore = Files.readString(Path.of(".gitignore"), StandardCharsets.UTF_8);
    assertTrue(gitignore.contains("application-local.properties"));
    assertTrue(gitignore.contains(".env"));

    String envExample = Files.readString(Path.of(".env.example"), StandardCharsets.UTF_8);
    assertTrue(envExample.lines().anyMatch(line -> line.trim().equals("MAIL_PASSWORD=")));
    assertTrue(envExample.lines().anyMatch(line -> line.trim().equals("SMTP_PASSWORD=")));

    String localExample =
        Files.readString(
            Path.of("src/main/resources/application-local.properties.example"),
            StandardCharsets.UTF_8);
    assertTrue(localExample.contains("spring.mail.password=your-app-password"));
  }
}
