package com.mailSender.mailtemplate;

import java.time.Instant;
import java.util.UUID;

public class TemplateResponse {

  private UUID id;
  private UUID organizationId;
  private String name;
  private String subject;
  private String body;
  private Instant createdAt;
  private Instant updatedAt;

  public TemplateResponse(
      UUID id,
      UUID organizationId,
      String name,
      String subject,
      String body,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.organizationId = organizationId;
    this.name = name;
    this.subject = subject;
    this.body = body;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getName() {
    return name;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
