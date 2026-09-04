package com.mailSender.smtp;

/** Sends a personalized message to one recipient. Implementations must not leak SMTP credentials. */
public interface EmailSender {

  void sendEmail(String to, String body);
}
