package com.mailSender.config;

/** SMTP or send preflight is missing required settings (credentials, from, sent-log, attachment). */
public class SmtpConfigurationException extends RuntimeException {

  public SmtpConfigurationException(String message) {
    super(message);
  }
}
