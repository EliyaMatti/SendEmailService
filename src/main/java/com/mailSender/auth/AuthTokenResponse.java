package com.mailSender.auth;

import java.util.UUID;

public class AuthTokenResponse {

  private String token;
  private UserResponse user;

  public AuthTokenResponse(String token, UserResponse user) {
    this.token = token;
    this.user = user;
  }

  public String getToken() {
    return token;
  }

  public UserResponse getUser() {
    return user;
  }

  public static class UserResponse {
    private UUID id;
    private String name;
    private String email;
    private UUID organizationId;
    private String role;

    public UserResponse(UUID id, String name, String email, UUID organizationId, String role) {
      this.id = id;
      this.name = name;
      this.email = email;
      this.organizationId = organizationId;
      this.role = role;
    }

    public UUID getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public String getEmail() {
      return email;
    }

    public UUID getOrganizationId() {
      return organizationId;
    }

    public String getRole() {
      return role;
    }
  }
}
