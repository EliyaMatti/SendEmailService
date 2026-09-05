package com.mailSender;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("api")
@TestPropertySource(
    properties = {"mail.batch-enabled=false", "mail.dry-run=true", "mail.test-send-enabled=false"})
class ApiProfileWebApplicationTest {

  @MockBean private JavaMailSender javaMailSender;

  @Autowired(required = false)
  private BatchMailRunner batchMailRunner;

  @LocalServerPort private int port;

  @Test
  void servletStartsAndBatchRunnerIsNotLoaded() {
    assertTrue(port > 0);
    assertNull(batchMailRunner);
  }
}
