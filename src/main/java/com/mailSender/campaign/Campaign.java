package com.mailSender.campaign;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
public class Campaign {

  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(nullable = false)
  private String name;

  @Column(name = "contact_list_id", nullable = false)
  private UUID contactListId;

  @Column(name = "template_id", nullable = false)
  private UUID templateId;

  @Column(name = "smtp_account_id", nullable = false)
  private UUID smtpAccountId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private CampaignStatus status = CampaignStatus.DRAFT;

  @Column(name = "total_recipients", nullable = false)
  private int totalRecipients;

  @Column(name = "queued_count", nullable = false)
  private int queuedCount;

  @Column(name = "sent_count", nullable = false)
  private int sentCount;

  @Column(name = "failed_count", nullable = false)
  private int failedCount;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(UUID organizationId) {
    this.organizationId = organizationId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public UUID getContactListId() {
    return contactListId;
  }

  public void setContactListId(UUID contactListId) {
    this.contactListId = contactListId;
  }

  public UUID getTemplateId() {
    return templateId;
  }

  public void setTemplateId(UUID templateId) {
    this.templateId = templateId;
  }

  public UUID getSmtpAccountId() {
    return smtpAccountId;
  }

  public void setSmtpAccountId(UUID smtpAccountId) {
    this.smtpAccountId = smtpAccountId;
  }

  public CampaignStatus getStatus() {
    return status;
  }

  public void setStatus(CampaignStatus status) {
    this.status = status;
  }

  public int getTotalRecipients() {
    return totalRecipients;
  }

  public void setTotalRecipients(int totalRecipients) {
    this.totalRecipients = totalRecipients;
  }

  public int getQueuedCount() {
    return queuedCount;
  }

  public void setQueuedCount(int queuedCount) {
    this.queuedCount = queuedCount;
  }

  public int getSentCount() {
    return sentCount;
  }

  public void setSentCount(int sentCount) {
    this.sentCount = sentCount;
  }

  public int getFailedCount() {
    return failedCount;
  }

  public void setFailedCount(int failedCount) {
    this.failedCount = failedCount;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(Instant startedAt) {
    this.startedAt = startedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Instant completedAt) {
    this.completedAt = completedAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
