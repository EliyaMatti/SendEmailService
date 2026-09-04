package com.mailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** UTF-8 body file load via {@link MailBody#readFileContent(String)}. */
class MailBodyReadFileTest {

  @TempDir Path tempDir;

  @Test
  void missingBodyFileFailsLoudly() {
    Path missing = tempDir.resolve("missing-body.txt");
    IllegalStateException ex =
        assertThrows(IllegalStateException.class, () -> MailBody.readFileContent(missing.toString()));
    assertTrue(ex.getMessage().contains("Cannot read body file"));
  }

  @Test
  void readsBodyAsUtf8() throws Exception {
    Path body = tempDir.resolve("body.txt");
    Files.writeString(body, "Hi {{name}} — café", StandardCharsets.UTF_8);
    assertEquals("Hi {{name}} — café", MailBody.readFileContent(body.toString()));
  }
}
