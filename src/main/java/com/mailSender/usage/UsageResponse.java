package com.mailSender.usage;

import java.time.LocalDate;
import java.util.UUID;

public class UsageResponse {

  private UUID organizationId;
  private LocalDate date;
  private int emailsAttempted;
  private int emailsSent;
  private int emailsFailed;
  private long campaignCount;
  private long contactCount;

  public UsageResponse(
      UUID organizationId,
      LocalDate date,
      int emailsAttempted,
      int emailsSent,
      int emailsFailed,
      long campaignCount,
      long contactCount) {
    this.organizationId = organizationId;
    this.date = date;
    this.emailsAttempted = emailsAttempted;
    this.emailsSent = emailsSent;
    this.emailsFailed = emailsFailed;
    this.campaignCount = campaignCount;
    this.contactCount = contactCount;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public LocalDate getDate() {
    return date;
  }

  public int getEmailsAttempted() {
    return emailsAttempted;
  }

  public int getEmailsSent() {
    return emailsSent;
  }

  public int getEmailsFailed() {
    return emailsFailed;
  }

  public long getCampaignCount() {
    return campaignCount;
  }

  public long getContactCount() {
    return contactCount;
  }
}
