package com.mailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MailBodyPersonalizeTest {

  @Test
  void missingKeysBecomeEmptyAndSpecialCharsAreLiteral() {
    EmailRecipient recipient = new EmailRecipient("a@example.com", "Ada $1 \\path");
    String result = MailBody.personalize("Hi {{name}} {{missing}} {{email}}", recipient);
    assertEquals("Hi Ada $1 \\path  a@example.com", result);
  }

  @Test
  void extraColumnPlaceholdersAreFilled() {
    EmailRecipient recipient =
        new EmailRecipient("a@example.com", "Ada", Map.of("company", "Acme"));
    assertEquals("Acme", MailBody.personalize("{{company}}", recipient));
  }
}
