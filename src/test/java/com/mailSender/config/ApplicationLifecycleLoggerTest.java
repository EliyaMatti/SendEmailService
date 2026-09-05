package com.mailSender.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class ApplicationLifecycleLoggerTest {

  @Test
  void logsStartupAndShutdownWithoutSecrets() {
    Logger logger = (Logger) LoggerFactory.getLogger(ApplicationLifecycleLogger.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      ApplicationLifecycleLogger lifecycle = new ApplicationLifecycleLogger();
      lifecycle.onStartup();
      lifecycle.onShutdown();
      List<String> messages =
          appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
      assertTrue(messages.contains("Application startup"));
      assertTrue(messages.contains("Application shutdown"));
      assertTrue(messages.stream().noneMatch(m -> m.toLowerCase().contains("password")));
    } finally {
      logger.detachAppender(appender);
    }
  }
}
