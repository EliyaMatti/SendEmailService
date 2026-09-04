package com.mailSender.template;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class TemplateValidatorTest {

  @Test
  void emptySubjectFails() {
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> TemplateValidator.validate("  ", "Hi {{name}}", Set.of("email", "name")));
    assertTrue(ex.getMessage().contains("Template validation failed"));
    assertTrue(ex.getMessage().contains("Subject is empty"));
  }

  @Test
  void emptyBodyFails() {
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> TemplateValidator.validate("Hello", "\n  \n", Set.of("email", "name")));
    assertTrue(ex.getMessage().contains("Body is empty"));
  }

  @Test
  void missingRequiredFieldsFails() {
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> TemplateValidator.validate("Hello", "Hi", Set.of("company")));
    assertTrue(ex.getMessage().contains("required fields: email and name"));
  }

  @Test
  void invalidPlaceholderSyntaxFails() {
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () ->
                TemplateValidator.validate(
                    "Hello", "Hi {{name", Set.of("email", "name")));
    assertTrue(ex.getMessage().contains("Invalid placeholder syntax"));
  }

  @Test
  void unsupportedPlaceholderFailsWithUsefulMessage() {
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () ->
                TemplateValidator.validate(
                    "Hello",
                    "Welcome to {{Company}}.",
                    Set.of("email", "name")));
    assertTrue(ex.getMessage().contains("Template validation failed"));
    assertTrue(
        ex.getMessage()
            .contains("Placeholder {{Company}} does not exist in the imported data."));
  }

  @Test
  void validTemplatePasses() {
    assertDoesNotThrow(
        () ->
            TemplateValidator.validate(
                "Hello",
                "Hi {{Name}}, welcome to {{company}}.",
                Set.of("email", "name", "company")));
  }
}
