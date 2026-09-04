package com.mailSender.template;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mailSender.excel.Contact;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateRendererTest {

  @Test
  void fillsMixedCasePlaceholdersFromContact() {
    Contact contact = new Contact("ada@example.com", "Rahul", Map.of("company", "ABC Ltd"));
    String template = "Hi {{Name}},\n\nWelcome to {{Company}}.\nSent to {{Email}}.";
    assertEquals(
        "Hi Rahul,\n\nWelcome to ABC Ltd.\nSent to ada@example.com.",
        TemplateRenderer.render(template, contact));
  }

  @Test
  void unknownPlaceholdersBecomeEmptyAndSpecialCharsAreLiteral() {
    Contact contact = new Contact("a@example.com", "Ada $1 \\path");
    assertEquals(
        "Hi Ada $1 \\path  a@example.com",
        TemplateRenderer.render("Hi {{name}} {{missing}} {{email}}", contact));
  }
}
