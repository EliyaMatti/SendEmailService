package com.mailSender.mailtemplate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class UpsertTemplateRequest {

  @NotBlank
  @Size(max = 255)
  private String name;

  @NotBlank
  @Size(max = 998)
  private String subject;

  @NotBlank private String body;

  private UUID contactListId;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public UUID getContactListId() {
    return contactListId;
  }

  public void setContactListId(UUID contactListId) {
    this.contactListId = contactListId;
  }
}
