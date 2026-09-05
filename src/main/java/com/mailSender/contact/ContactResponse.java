package com.mailSender.contact;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class ContactResponse {

  private UUID id;
  private UUID contactListId;
  private String email;
  private String name;
  private ContactStatus status;
  private Map<String, String> metadata;
  private Instant createdAt;

  public ContactResponse(
      UUID id,
      UUID contactListId,
      String email,
      String name,
      ContactStatus status,
      Map<String, String> metadata,
      Instant createdAt) {
    this.id = id;
    this.contactListId = contactListId;
    this.email = email;
    this.name = name;
    this.status = status;
    this.metadata = metadata;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getContactListId() {
    return contactListId;
  }

  public String getEmail() {
    return email;
  }

  public String getName() {
    return name;
  }

  public ContactStatus getStatus() {
    return status;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
