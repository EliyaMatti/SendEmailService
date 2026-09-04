package com.mailSender.smtp;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.AddressException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.UnresolvedAddressException;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;

final class SmtpFailureClassifier {

  private SmtpFailureClassifier() {}

  static String userMessage(String to, Throwable error) {
    if (matches(error, SmtpFailureClassifier::isAuthentication)) {
      return "SMTP authentication failed. Check MAIL_USERNAME and MAIL_PASSWORD (for Gmail, use an App Password).";
    }
    if (matches(error, SmtpFailureClassifier::isTimeout)) {
      return "SMTP timed out while sending to " + to + ".";
    }
    if (matches(error, SmtpFailureClassifier::isConnection)) {
      return "Could not connect to the SMTP server. Check MAIL_HOST, MAIL_PORT, TLS, and network access.";
    }
    if (matches(error, SmtpFailureClassifier::isInvalidRecipient)) {
      return "SMTP rejected recipient " + to + " as invalid.";
    }
    if (matches(error, SmtpFailureClassifier::isConfiguration)) {
      String detail = firstSafeDetail(error);
      if (detail != null && !detail.isBlank()) {
        return "SMTP configuration error: " + detail;
      }
      return "SMTP configuration error. Check from address, host, port, and TLS settings.";
    }
    if (matches(error, SmtpFailureClassifier::isRejection)) {
      return "SMTP server rejected the message to " + to + ".";
    }
    return "Failed to send email to " + to + ".";
  }

  private static boolean matches(Throwable error, java.util.function.Predicate<Throwable> test) {
    Throwable current = error;
    while (current != null) {
      if (test.test(current)) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private static boolean isAuthentication(Throwable error) {
    if (error instanceof MailAuthenticationException
        || error instanceof AuthenticationFailedException) {
      return true;
    }
    String text = safeLower(error.getMessage());
    return text.contains("authentication failed")
        || text.contains("535")
        || text.contains("username and password not accepted");
  }

  private static boolean isTimeout(Throwable error) {
    if (error instanceof SocketTimeoutException) {
      return true;
    }
    String text = safeLower(error.getMessage());
    return text.contains("timed out") || text.contains("timeout");
  }

  private static boolean isConnection(Throwable error) {
    return error instanceof ConnectException
        || error instanceof UnknownHostException
        || error instanceof UnresolvedAddressException;
  }

  private static boolean isInvalidRecipient(Throwable error) {
    if (error instanceof AddressException) {
      return true;
    }
    if (error instanceof SendFailedException sendFailed) {
      return sendFailed.getInvalidAddresses() != null && sendFailed.getInvalidAddresses().length > 0;
    }
    String text = safeLower(error.getMessage());
    return text.contains("invalid addresses") || text.contains("invalid address");
  }

  private static boolean isConfiguration(Throwable error) {
    if (error instanceof MailParseException) {
      return true;
    }
    String text = safeLower(error.getMessage());
    return text.contains("cannot read file")
        || text.contains("illegal address")
        || text.contains("failed to parse");
  }

  private static boolean isRejection(Throwable error) {
    if (error instanceof MailSendException || error instanceof SendFailedException) {
      return true;
    }
    String text = safeLower(error.getMessage());
    return text.contains("550")
        || text.contains("553")
        || text.contains("554")
        || text.contains("rejected");
  }

  private static String firstSafeDetail(Throwable error) {
    Throwable current = error;
    while (current != null) {
      String message = current.getMessage();
      if (message != null && !message.isBlank() && !looksLikeStackTrace(message)) {
        return message.strip();
      }
      current = current.getCause();
    }
    return null;
  }

  private static boolean looksLikeStackTrace(String message) {
    return message.contains("\n\tat ") || message.contains("Exception in thread");
  }

  private static String safeLower(String message) {
    return message == null ? "" : message.toLowerCase();
  }
}
