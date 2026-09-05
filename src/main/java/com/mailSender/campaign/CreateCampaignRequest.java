package com.mailSender.campaign;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class CreateCampaignRequest {

  @NotBlank
  @Size(max = 255)
  private String name;

  @NotNull private UUID contactListId;
  @NotNull private UUID templateId;
  @NotNull private UUID smtpAccountId;

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
}
