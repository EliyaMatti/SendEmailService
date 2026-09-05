package com.mailSender.smtp;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmailSendingExceptionTest {

  @Test
  void smtpSendExceptionIsEmailSendingException() {
    SmtpSendException ex = new SmtpSendException("auth failed", new RuntimeException("cause"));
    assertTrue(ex instanceof EmailSendingException);
    assertTrue(ex.getMessage().contains("auth failed"));
  }
}
