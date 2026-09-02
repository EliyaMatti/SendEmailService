package com.mailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mail.javamail.MimeMessageHelper;

class MailBodyAttachmentTest {

  @TempDir Path tempDir;

  @Test
  void omittedAttachmentPathIsIgnored() {
    MailAppProperties properties = new MailAppProperties();
    properties.setAttachmentPath("");
    MailBodyAttachment attachment = new MailBodyAttachment(properties);
    assertDoesNotThrow(() -> attachment.addAttachment(mock(MimeMessageHelper.class)));
  }

  @Test
  void missingAttachmentFileFails() {
    MailAppProperties properties = new MailAppProperties();
    properties.setAttachmentPath(tempDir.resolve("missing.pdf").toString());
    MailBodyAttachment attachment = new MailBodyAttachment(properties);
    MimeMessageHelper helper = mock(MimeMessageHelper.class);
    assertThrows(RuntimeException.class, () -> attachment.addAttachment(helper));
  }
}
