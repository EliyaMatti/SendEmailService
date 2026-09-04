package com.mailSender.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Structured lifecycle logs required by M1-018. Does not log credentials. */
@Component
public class ApplicationLifecycleLogger {

  private static final Logger log = LoggerFactory.getLogger(ApplicationLifecycleLogger.class);

  @EventListener(ApplicationReadyEvent.class)
  public void onStartup() {
    log.info("Application startup");
  }

  @EventListener(ContextClosedEvent.class)
  public void onShutdown() {
    log.info("Application shutdown");
  }
}
