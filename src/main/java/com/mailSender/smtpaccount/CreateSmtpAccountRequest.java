package com.mailSender.smtpaccount;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateSmtpAccountRequest {

  @NotBlank
  @Size(max = 64)
  private String provider;

  @NotBlank
  @Size(max = 255)
  private String host;

  @Min(1)
  @Max(65535)
  private int port = 587;

  @NotBlank
  @Size(max = 320)
  private String username;

  @NotBlank
  @Size(max = 512)
  private String password;

  @NotBlank
  @Email
  @Size(max = 320)
  private String fromEmail;

  @Size(max = 255)
  private String fromName;

  private boolean tlsEnabled = true;

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getFromEmail() {
    return fromEmail;
  }

  public void setFromEmail(String fromEmail) {
    this.fromEmail = fromEmail;
  }

  public String getFromName() {
    return fromName;
  }

  public void setFromName(String fromName) {
    this.fromName = fromName;
  }

  public boolean isTlsEnabled() {
    return tlsEnabled;
  }

  public void setTlsEnabled(boolean tlsEnabled) {
    this.tlsEnabled = tlsEnabled;
  }
}
