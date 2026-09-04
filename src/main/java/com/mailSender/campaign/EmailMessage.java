package com.mailSender.campaign;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Domain model for one outbound email (to, subject, body, from, reply-to, attachment paths). It
 * carries no SMTP or JavaMail types so composition stays independent of the transport.
 */
public final class EmailMessage {

  private final String to;
  private final String subject;
  private final String body;
  private final String from;
  private final String replyTo;
  private final List<String> attachments;

  public EmailMessage(
      String to, String subject, String body, String from, String replyTo, List<String> attachments) {
    this.to = nullToEmpty(to);
    this.subject = nullToEmpty(subject);
    this.body = nullToEmpty(body);
    this.from = nullToEmpty(from);
    this.replyTo = nullToEmpty(replyTo);
    this.attachments = copyAttachments(attachments);
  }

  public String getTo() {
    return to;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  public String getFrom() {
    return from;
  }

  public String getReplyTo() {
    return replyTo;
  }

  public List<String> getAttachments() {
    return attachments;
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private static List<String> copyAttachments(List<String> attachments) {
    if (attachments == null || attachments.isEmpty()) {
      return List.of();
    }
    List<String> copy = new ArrayList<>();
    for (String path : attachments) {
      if (path != null && !path.isBlank()) {
        copy.add(path);
      }
    }
    return Collections.unmodifiableList(copy);
  }
}
