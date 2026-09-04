package com.mailSender.template;

/** Subject, body, or placeholders failed validation, or the body file could not be read. */
public class TemplateValidationException extends RuntimeException {

  public TemplateValidationException(String message) {
    super(message);
  }

  public TemplateValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}
