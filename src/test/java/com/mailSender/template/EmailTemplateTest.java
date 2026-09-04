package com.mailSender.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmailTemplateTest {

  @TempDir Path tempDir;

  @Test
  void missingBodyFileFailsLoudly() {
    Path missing = tempDir.resolve("missing-body.txt");
    TemplateValidationException ex =
        assertThrows(TemplateValidationException.class, () -> EmailTemplate.load(missing.toString()));
    assertTrue(ex.getMessage().contains("Unable to read the email body file"));
  }

  @Test
  void loadsBodyAsUtf8() throws Exception {
    Path body = tempDir.resolve("body.txt");
    Files.writeString(body, "Hi {{name}} — café", StandardCharsets.UTF_8);
    assertEquals("Hi {{name}} — café", EmailTemplate.load(body.toString()).getBody());
  }
}
