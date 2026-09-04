package com.mailSender.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for the SMTP-independent {@link EmailMessage} fields and attachment list. */
class EmailMessageTest {

  @Test
  void storesEnvelopeAndBodyIndependentlyOfSmtpTypes() {
    EmailMessage message =
        new EmailMessage(
            "to@example.com",
            "Subject",
            "Hello",
            "from@example.com",
            "reply@example.com",
            List.of("resume.pdf"));

    assertEquals("to@example.com", message.getTo());
    assertEquals("Subject", message.getSubject());
    assertEquals("Hello", message.getBody());
    assertEquals("from@example.com", message.getFrom());
    assertEquals("reply@example.com", message.getReplyTo());
    assertEquals(List.of("resume.pdf"), message.getAttachments());
  }

  @Test
  void nullAndBlankAttachmentsAreOmittedAndListIsUnmodifiable() {
    EmailMessage message =
        new EmailMessage(
            "to@example.com", null, null, null, null, java.util.Arrays.asList(" ", "a.pdf", null));
    assertEquals("", message.getSubject());
    assertEquals("", message.getBody());
    assertEquals("", message.getFrom());
    assertEquals("", message.getReplyTo());
    assertEquals(List.of("a.pdf"), message.getAttachments());
    assertThrows(UnsupportedOperationException.class, () -> message.getAttachments().add("x.pdf"));

    List<String> mutable = new ArrayList<>();
    mutable.add("b.pdf");
    EmailMessage copy = new EmailMessage("to@example.com", "s", "b", "f", "", mutable);
    mutable.clear();
    assertEquals(List.of("b.pdf"), copy.getAttachments());
  }
}
