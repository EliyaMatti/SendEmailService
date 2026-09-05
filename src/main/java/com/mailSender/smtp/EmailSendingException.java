package com.mailSender.smtp;

/** A send attempt or campaign send loop failed. */
public class EmailSendingException extends RuntimeException {

  public EmailSendingException(String message) {
    super(message);
  }

  public EmailSendingException(String message, Throwable cause) {
    super(message, cause);
  }
}
