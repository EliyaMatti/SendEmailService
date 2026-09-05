package com.mailSender.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MailAppPropertiesTest {

  @Test
  void javaDefaultsMatchSafeNamedConfig() {
    MailAppProperties properties = new MailAppProperties();
    assertFalse(properties.isBatchEnabled());
    assertTrue(properties.isDryRun());
    assertEquals(1000L, properties.getSendDelayMs());
    assertEquals("sent-addresses.txt", properties.getSentLogPath());
    assertFalse(properties.isTestSendEnabled());
    assertEquals("", properties.getFromName());
  }
}
