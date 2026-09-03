package com.mailSender;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SentAddressLog {

  private final MailAppProperties mailAppProperties;

  public SentAddressLog(MailAppProperties mailAppProperties) {
    this.mailAppProperties = mailAppProperties;
  }

  public Set<String> load() {
    Path path = logPath();
    if (path == null || !Files.isRegularFile(path)) {
      return Set.of();
    }
    try {
      return Files.readAllLines(path, StandardCharsets.UTF_8).stream()
          .map(SentAddressLog::normalize)
          .filter(s -> !s.isEmpty())
          .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), Collections::unmodifiableSet));
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read sent log: " + path, e);
    }
  }

  public void record(String email) {
    Path path = logPath();
    if (path == null) {
      return;
    }
    try {
      Path parent = path.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      Files.writeString(
          path,
          normalize(email) + System.lineSeparator(),
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot write sent log: " + path, e);
    }
  }

  static String normalize(String email) {
    return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
  }

  private Path logPath() {
    String configured = mailAppProperties.getSentLogPath();
    if (configured == null || configured.isBlank()) {
      return null;
    }
    return Path.of(configured);
  }
}
