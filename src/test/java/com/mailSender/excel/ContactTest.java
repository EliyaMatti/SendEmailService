package com.mailSender.excel;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ContactTest {

  @Test
  void placeholdersAreUnmodifiable() {
    Contact contact = new Contact("a@example.com", "Ada", Map.of("company", "Acme"));
    assertThrows(
        UnsupportedOperationException.class, () -> contact.getPlaceholders().put("x", "y"));
  }
}
