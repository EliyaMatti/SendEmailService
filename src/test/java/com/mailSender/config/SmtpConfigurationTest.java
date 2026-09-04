package com.mailSender.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
    assertTrue(config.isReadyForSend());
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
