package com.mailSender.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mailSender.config.MailAppProperties;
import com.mailSender.excel.Contact;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Composer tests: Contact + template become an {@link EmailMessage} with no SMTP involved. */
class EmailComposerTest {

  @Test
  void rendersTemplateAndCopiesEnvelopeWithoutSending() {
    MailAppProperties properties = new MailAppProperties();
    properties.setSubject("Hello");
    properties.setFrom("from@example.com");
    properties.setAttachmentPath("resume.pdf");

    EmailComposer composer = new EmailComposer(properties);
    EmailMessage message =
        composer.compose(new Contact("ada@example.com", "Ada", Map.of("company", "Acme")), "Hi {{Name}} at {{company}}");

    assertEquals("ada@example.com", message.getTo());
    assertEquals("Hello", message.getSubject());
    assertEquals("Hi Ada at Acme", message.getBody());
    assertEquals("from@example.com", message.getFrom());
    assertEquals("", message.getReplyTo());
    assertEquals(java.util.List.of("resume.pdf"), message.getAttachments());
  }
}
