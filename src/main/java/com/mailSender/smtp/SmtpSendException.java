package com.mailSender.smtp;

/** User-facing SMTP failure. The message is safe to log; the cause is for debug only. */
public class SmtpSendException extends RuntimeException {

  public SmtpSendException(String message, Throwable cause) {
    super(message, cause);
  }
}
