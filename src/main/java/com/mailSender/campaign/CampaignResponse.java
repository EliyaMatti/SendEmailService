package com.mailSender.campaign;

import java.time.Instant;
import java.util.UUID;

public class CampaignResponse {

  private UUID id;
  private UUID organizationId;
  private String name;
  private UUID contactListId;
  private UUID templateId;
  private UUID smtpAccountId;
  private CampaignStatus status;
  private int totalRecipients;
  private int queuedCount;
  private int sentCount;
  private int failedCount;
  private Instant createdAt;
  private Instant startedAt;
  private Instant completedAt;
  private Instant updatedAt;

  public CampaignResponse(
      UUID id,
      UUID organizationId,
      String name,
      UUID contactListId,
      UUID templateId,
      UUID smtpAccountId,
      CampaignStatus status,
      int totalRecipients,
      int queuedCount,
      int sentCount,
      int failedCount,
      Instant createdAt,
      Instant startedAt,
      Instant completedAt,
      Instant updatedAt) {
    this.id = id;
    this.organizationId = organizationId;
    this.name = name;
    this.contactListId = contactListId;
    this.templateId = templateId;
    this.smtpAccountId = smtpAccountId;
    this.status = status;
    this.totalRecipients = totalRecipients;
    this.queuedCount = queuedCount;
    this.sentCount = sentCount;
    this.failedCount = failedCount;
    this.createdAt = createdAt;
    this.startedAt = startedAt;
    this.completedAt = completedAt;
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

  public UUID getContactListId() {
    return contactListId;
  }

  public UUID getTemplateId() {
    return templateId;
  }

  public UUID getSmtpAccountId() {
    return smtpAccountId;
  }

  public CampaignStatus getStatus() {
    return status;
  }

  public int getTotalRecipients() {
    return totalRecipients;
  }

  public int getQueuedCount() {
    return queuedCount;
  }

  public int getSentCount() {
    return sentCount;
  }

  public int getFailedCount() {
    return failedCount;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
