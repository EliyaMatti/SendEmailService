package com.mailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.mailSender.smtp.EmailSendingException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mail.javamail.MimeMessageHelper;

/** Attachment helper: omitted paths are ignored; a missing file fails that message. */
class MailBodyAttachmentTest {

  @TempDir Path tempDir;

  @Test
  void omittedAttachmentPathIsIgnored() {
    MailBodyAttachment attachment = new MailBodyAttachment();
    assertDoesNotThrow(() -> attachment.addAttachments(mock(MimeMessageHelper.class), List.of()));
  }

  @Test
  void blankAndNullAttachmentPathsAreSkipped() {
    MailBodyAttachment attachment = new MailBodyAttachment();
    assertDoesNotThrow(
        () ->
            attachment.addAttachments(
                mock(MimeMessageHelper.class), java.util.Arrays.asList(null, "  ")));
  }

  @Test
  void missingAttachmentFileFails() {
    MailBodyAttachment attachment = new MailBodyAttachment();
    MimeMessageHelper helper = mock(MimeMessageHelper.class);
    assertThrows(
        EmailSendingException.class,
        () ->
            attachment.addAttachments(
                helper, List.of(tempDir.resolve("missing.pdf").toString())));
  }
}
