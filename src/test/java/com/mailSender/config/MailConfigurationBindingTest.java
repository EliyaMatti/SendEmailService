package com.mailSender.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;

/**
 * Named {@code mail.*} / {@code spring.mail.*} / logging keys bind from configuration (batch off,
 * no live SMTP).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(
    properties = {
      "mail.batch-enabled=false",
      "mail.dry-run=true",
      "mail.test-send-enabled=false",
      "mail.from=from@example.com",
      "mail.from-name=ExcelMail",
      "mail.excel-file-path=/tmp/recipients.xlsx",
      "mail.body-file-path=/tmp/body.txt",
      "mail.send-delay-ms=2500",
      "spring.mail.host=smtp.example.test",
      "spring.mail.port=2525",
      "spring.mail.properties.mail.smtp.auth=true",
      "spring.mail.properties.mail.smtp.starttls.enable=true",
      "logging.level.com.mailSender=WARN"
    })
class MailConfigurationBindingTest {

  @MockBean private JavaMailSender javaMailSender;

  @Autowired private MailAppProperties mailAppProperties;
  @Autowired private SmtpConfiguration smtpConfiguration;

  @Test
  void centralizesSendingInputAndSmtpSettings() {
    assertEquals("from@example.com", mailAppProperties.getFrom());
    assertEquals("ExcelMail", mailAppProperties.getFromName());
    assertEquals("/tmp/recipients.xlsx", mailAppProperties.getExcelFilePath());
    assertEquals("/tmp/body.txt", mailAppProperties.getBodyFilePath());
    assertEquals(2500L, mailAppProperties.getSendDelayMs());
    assertTrue(mailAppProperties.isDryRun());
    assertEquals("smtp.example.test", smtpConfiguration.getHost());
    assertEquals(2525, smtpConfiguration.getPort());
    assertEquals("from@example.com", smtpConfiguration.getFromEmail());
    assertEquals("ExcelMail", smtpConfiguration.getFromName());
    assertTrue(smtpConfiguration.isTlsEnabled());
    assertTrue(smtpConfiguration.isAuthEnabled());
  }
}
