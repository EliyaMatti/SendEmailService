package com.mailSender.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mailSender.excel.Contact;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TemplateRendererTest {

  private static final Contact ADA =
      new Contact("ada@example.com", "Ada", Map.of("company", "Acme"));

  @Test
  @DisplayName("{{Name}}")
  void rendersNamePlaceholder() {
    assertEquals("Hello Ada", TemplateRenderer.render("Hello {{Name}}", ADA));
  }

  @Test
  @DisplayName("{{Email}}")
  void rendersEmailPlaceholder() {
    assertEquals("To: ada@example.com", TemplateRenderer.render("To: {{Email}}", ADA));
  }

  @Test
  @DisplayName("{{Company}}")
  void rendersCompanyPlaceholder() {
    assertEquals("At Acme", TemplateRenderer.render("At {{Company}}", ADA));
  }

  @Test
  @DisplayName("Multiple placeholders")
  void rendersNameEmailAndCompanyTogether() {
    String template = "Hi {{Name}},\n\nWelcome to {{Company}}.\nSent to {{Email}}.";
    assertEquals(
        "Hi Ada,\n\nWelcome to Acme.\nSent to ada@example.com.",
        TemplateRenderer.render(template, ADA));
  }

  @Test
  @DisplayName("Missing field")
  void missingExcelColumnBecomesEmpty() {
    Contact withoutCompany = new Contact("ada@example.com", "Ada");
    assertEquals("Hi Ada at ", TemplateRenderer.render("Hi {{Name}} at {{Company}}", withoutCompany));
  }

  @Test
  @DisplayName("Unknown placeholder")
  void unknownPlaceholderBecomesEmpty() {
    assertEquals("Hi Ada", TemplateRenderer.render("Hi {{Name}}{{Unknown}}", ADA));
  }

  @Test
  @DisplayName("Empty value")
  void emptyPlaceholderValueIsInsertedAsBlank() {
    Contact emptyCompany = new Contact("ada@example.com", "Ada", Map.of("company", ""));
    assertEquals("Co: .", TemplateRenderer.render("Co: {{Company}}.", emptyCompany));
  }

  @Test
  @DisplayName("Empty template")
  void emptyTemplateStaysEmpty() {
    assertEquals("", TemplateRenderer.render("", ADA));
  }

  @Test
  void unknownPlaceholdersBecomeEmptyAndSpecialCharsAreLiteral() {
    Contact contact = new Contact("a@example.com", "Ada $1 \\path");
    assertEquals(
        "Hi Ada $1 \\path  a@example.com",
        TemplateRenderer.render("Hi {{name}} {{missing}} {{email}}", contact));
  }

  @Test
  void nullTemplateIsReturnedUnchanged() {
    assertNull(TemplateRenderer.render(null, ADA));
  }
}
