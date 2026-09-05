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
@ActiveProfiles("development")
@TestPropertySource(properties = {"mail.test-send-enabled=false"})
class MailDevelopmentProfileTest {

  @MockBean private JavaMailSender javaMailSender;

  @Autowired private MailAppProperties mailAppProperties;
  @Autowired private Environment environment;

  @Test
  void developmentKeepsBatchOffAndDryRunOn() {
    assertTrue(Arrays.asList(environment.getActiveProfiles()).contains("development"));
    assertFalse(mailAppProperties.isBatchEnabled());
    assertTrue(mailAppProperties.isDryRun());
    assertFalse(mailAppProperties.isTestSendEnabled());
  }
}
