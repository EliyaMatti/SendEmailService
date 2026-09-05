package com.mailSender.smtpaccount;

import java.time.Instant;
import java.util.UUID;

public class SmtpAccountResponse {

  private UUID id;
  private UUID organizationId;
  private String provider;
  private String host;
  private int port;
  private String username;
  private String fromEmail;
  private String fromName;
  private boolean tlsEnabled;
  private Instant createdAt;
  private Instant updatedAt;

  public SmtpAccountResponse(
      UUID id,
      UUID organizationId,
      String provider,
      String host,
      int port,
      String username,
      String fromEmail,
      String fromName,
      boolean tlsEnabled,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.organizationId = organizationId;
    this.provider = provider;
    this.host = host;
    this.port = port;
    this.username = username;
    this.fromEmail = fromEmail;
    this.fromName = fromName;
    this.tlsEnabled = tlsEnabled;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getProvider() {
    return provider;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getUsername() {
    return username;
  }

  public String getFromEmail() {
    return fromEmail;
  }

  public String getFromName() {
    return fromName;
  }

  public boolean isTlsEnabled() {
    return tlsEnabled;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
