package com.mailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mailSender.config.MailAppProperties;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SentAddressLogTest {

  @TempDir Path tempDir;

  @Test
  void loadAndRecordRoundTrip() throws Exception {
    Path logFile = tempDir.resolve("sent.txt");
    MailAppProperties properties = new MailAppProperties();
    properties.setSentLogPath(logFile.toString());
    SentAddressLog log = new SentAddressLog(properties);

    assertTrue(log.load().isEmpty());
    log.record("  A@Example.com ");
    assertEquals(Set.of("a@example.com"), log.load());
    assertEquals("a@example.com" + System.lineSeparator(), Files.readString(logFile, StandardCharsets.UTF_8));
  }

  @Test
  void blankPathIsNoOp() {
    MailAppProperties properties = new MailAppProperties();
    properties.setSentLogPath("");
    SentAddressLog log = new SentAddressLog(properties);
    log.record("a@example.com");
    assertTrue(log.load().isEmpty());
  }
}
