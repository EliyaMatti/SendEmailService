package com.mailSender;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;

/**
 * Spring context smoke test: batch and test-send stay off so no Excel or SMTP work runs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {"mail.batch-enabled=false", "mail.dry-run=true", "mail.test-send-enabled=false"})
class MailSenderApplicationTests {

  @MockBean private JavaMailSender javaMailSender;

  @Test
  void contextLoads() {}
}
