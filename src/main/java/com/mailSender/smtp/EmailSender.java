package com.mailSender.smtp;

import com.mailSender.campaign.EmailMessage;

/** Sends a composed {@link EmailMessage}. Implementations must not leak SMTP credentials. */
public interface EmailSender {

  void send(EmailMessage message);
}
