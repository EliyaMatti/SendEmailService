package com.mailSender.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;

/**
 * Defaults come from {@code application.properties} (batch off, dry-run on, no live SMTP).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(
    properties = {"mail.batch-enabled=false", "mail.dry-run=true", "mail.test-send-enabled=false"})
class MailCentralConfigTest {

  @MockBean private JavaMailSender javaMailSender;

  @Autowired private MailAppProperties mailAppProperties;
  @Autowired private SmtpConfiguration smtpConfiguration;

  @Test
  void applicationPropertiesBindSmtpFilesSendingAndTls() {
    assertFalse(mailAppProperties.isBatchEnabled());
    assertTrue(mailAppProperties.isDryRun());
    assertEquals(1000L, mailAppProperties.getSendDelayMs());
    assertEquals("smtp.gmail.com", smtpConfiguration.getHost());
    assertEquals(587, smtpConfiguration.getPort());
    assertTrue(smtpConfiguration.isTlsEnabled());
    assertTrue(smtpConfiguration.isAuthEnabled());
  }
}
