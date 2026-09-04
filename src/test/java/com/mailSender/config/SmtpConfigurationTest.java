package com.mailSender.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.mail.MailProperties;

class SmtpConfigurationTest {

  @Test
  void toStringDoesNotExposePassword() {
    SmtpConfiguration config =
        new SmtpConfiguration(
            "smtp.example.com",
            587,
            "user@example.com",
            "super-secret-app-password",
            "from@example.com",
            "Sender",
            true);
    String text = config.toString();
    assertFalse(text.contains("super-secret-app-password"));
    assertTrue(text.contains("password=***"));
    assertTrue(text.contains("smtp.example.com"));
    assertTrue(text.contains("user@example.com"));
    assertTrue(text.contains("from@example.com"));
  }

  @Test
  void gettersReturnConfiguredValues() {
    SmtpConfiguration config =
        new SmtpConfiguration(
            "smtp.example.com", 465, "user", "secret", "from@example.com", "From Name", false);
    assertEquals("smtp.example.com", config.getHost());
    assertEquals(465, config.getPort());
    assertEquals("user", config.getUsername());
    assertEquals("secret", config.getPassword());
    assertEquals("from@example.com", config.getFromEmail());
    assertEquals("From Name", config.getFromName());
    assertFalse(config.isTlsEnabled());
    assertTrue(config.isAuthEnabled());
    assertTrue(config.isReadyForSend());
  }

  @Test
  void bindsFromSpringMailPropertiesAndMailAppProperties() {
    MailProperties mail = new MailProperties();
    mail.setHost("smtp.example.com");
    mail.setPort(465);
    mail.setUsername("user");
    mail.setPassword("secret");
    mail.getProperties().put("mail.smtp.starttls.enable", "false");
    mail.getProperties().put("mail.smtp.auth", "false");
    MailAppProperties app = new MailAppProperties();
    app.setFrom("from@example.com");
    app.setFromName("From Name");
    SmtpConfiguration config = new SmtpConfiguration(mail, app);
    assertEquals("smtp.example.com", config.getHost());
    assertEquals(465, config.getPort());
    assertEquals("user", config.getUsername());
    assertEquals("from@example.com", config.getFromEmail());
    assertEquals("From Name", config.getFromName());
    assertFalse(config.isTlsEnabled());
    assertFalse(config.isAuthEnabled());
  }

  @Test
  void readyForSendRequiresUsernamePasswordAndFrom() {
    assertFalse(smtp("", "secret", "from@example.com").isReadyForSend());
    assertFalse(smtp("user", "", "from@example.com").isReadyForSend());
    assertFalse(smtp("user", "secret", "").isReadyForSend());
    assertTrue(smtp("user", "secret", "from@example.com").isReadyForSend());
  }

  private static SmtpConfiguration smtp(String username, String password, String fromEmail) {
    return new SmtpConfiguration("smtp.gmail.com", 587, username, password, fromEmail, "", true);
  }
}
