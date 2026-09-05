package com.mailSender.contact;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ContactListResponse {

  private UUID id;
  private UUID organizationId;
  private String name;
  private String sourceFilename;
  private int totalContacts;
  private List<String> placeholderKeys;
  private Instant createdAt;
  private Instant updatedAt;

  public ContactListResponse(
      UUID id,
      UUID organizationId,
      String name,
      String sourceFilename,
      int totalContacts,
      List<String> placeholderKeys,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.organizationId = organizationId;
    this.name = name;
    this.sourceFilename = sourceFilename;
    this.totalContacts = totalContacts;
    this.placeholderKeys = placeholderKeys;
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

  public String getSourceFilename() {
    return sourceFilename;
  }

  public int getTotalContacts() {
    return totalContacts;
  }

  public List<String> getPlaceholderKeys() {
    return placeholderKeys;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
