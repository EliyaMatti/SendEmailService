package com.mailSender.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("production")
@TestPropertySource(
    properties = {
      "mail.batch-enabled=false",
      "mail.dry-run=true",
      "mail.test-send-enabled=false",
      "spring.mail.username=",
      "spring.mail.password="
    })
class MailProductionProfileTest {

  @MockBean private JavaMailSender javaMailSender;

  @Autowired private MailAppProperties mailAppProperties;
  @Autowired private SmtpConfiguration smtpConfiguration;
  @Autowired private Environment environment;

  @Test
  void productionProfileLoadsWithSafeSendDefaultsWhenCredentialsAreUnset() {
    assertTrue(Arrays.asList(environment.getActiveProfiles()).contains("production"));
    assertFalse(mailAppProperties.isBatchEnabled());
    assertTrue(mailAppProperties.isDryRun());
    assertTrue(smtpConfiguration.getUsername().isBlank());
    assertTrue(smtpConfiguration.getPassword().isBlank());
  }
}
