package com.mailSender;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class EmailRecipientTest {

  @Test
  void placeholdersAreUnmodifiable() {
    EmailRecipient recipient =
        new EmailRecipient("a@example.com", "Ada", Map.of("company", "Acme"));
    assertThrows(
        UnsupportedOperationException.class,
        () -> recipient.getPlaceholders().put("x", "y"));
  }
}
