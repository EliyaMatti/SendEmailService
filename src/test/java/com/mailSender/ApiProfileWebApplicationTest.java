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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"api", "apitest"})
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
