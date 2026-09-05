package com.mailSender.usage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "usage_records")
public class UsageRecord {

  @Id private UUID id;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "usage_date", nullable = false)
  private LocalDate usageDate;

  @Column(name = "emails_attempted", nullable = false)
  private int emailsAttempted;

  @Column(name = "emails_sent", nullable = false)
  private int emailsSent;

  @Column(name = "emails_failed", nullable = false)
  private int emailsFailed;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

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

  public UUID getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(UUID organizationId) {
    this.organizationId = organizationId;
  }

  public LocalDate getUsageDate() {
    return usageDate;
  }

  public void setUsageDate(LocalDate usageDate) {
    this.usageDate = usageDate;
  }

  public int getEmailsAttempted() {
    return emailsAttempted;
  }

  public void setEmailsAttempted(int emailsAttempted) {
    this.emailsAttempted = emailsAttempted;
  }

  public int getEmailsSent() {
    return emailsSent;
  }

  public void setEmailsSent(int emailsSent) {
    this.emailsSent = emailsSent;
  }

  public int getEmailsFailed() {
    return emailsFailed;
  }

  public void setEmailsFailed(int emailsFailed) {
    this.emailsFailed = emailsFailed;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
