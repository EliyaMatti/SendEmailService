package com.mailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mailSender.excel.Contact;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Template personalization still delegates to {@link com.mailSender.template.TemplateRenderer}. */
class MailBodyPersonalizeTest {

  @Test
  void missingKeysBecomeEmptyAndSpecialCharsAreLiteral() {
    Contact recipient = new Contact("a@example.com", "Ada $1 \\path");
    String result = MailBody.personalize("Hi {{name}} {{missing}} {{email}}", recipient);
    assertEquals("Hi Ada $1 \\path  a@example.com", result);
  }

  @Test
  void extraColumnPlaceholdersAreFilled() {
    Contact recipient = new Contact("a@example.com", "Ada", Map.of("company", "Acme"));
    assertEquals("Acme", MailBody.personalize("{{company}}", recipient));
  }

  @Test
  void mixedCasePlaceholderNamesAreFilled() {
    Contact recipient = new Contact("a@example.com", "Ada", Map.of("company", "Acme"));
    assertEquals("Ada at Acme", MailBody.personalize("{{Name}} at {{Company}}", recipient));
  }
}
