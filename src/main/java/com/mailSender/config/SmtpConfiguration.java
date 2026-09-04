package com.mailSender.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SMTP connection and envelope settings. The password is held in memory only and is never included
 * in {@link #toString()}.
 */
@Component
public class SmtpConfiguration {

  private final String host;
  private final int port;
  private final String username;
  private final String password;
  private final String fromEmail;
  private final String fromName;
  private final boolean tlsEnabled;

  public SmtpConfiguration(
      @Value("${spring.mail.host:smtp.gmail.com}") String host,
      @Value("${spring.mail.port:587}") int port,
      @Value("${spring.mail.username:}") String username,
      @Value("${spring.mail.password:}") String password,
      @Value("${mail.from:}") String fromEmail,
      @Value("${mail.from-name:}") String fromName,
      @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}") boolean tlsEnabled) {
    this.host = host == null ? "" : host;
    this.port = port;
    this.username = username == null ? "" : username;
    this.password = password == null ? "" : password;
    this.fromEmail = fromEmail == null ? "" : fromEmail;
    this.fromName = fromName == null ? "" : fromName;
    this.tlsEnabled = tlsEnabled;
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

  public String getPassword() {
    return password;
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

  public boolean isReadyForSend() {
    return !isBlank(username) && !isBlank(password) && !isBlank(fromEmail);
  }

  @Override
  public String toString() {
    return "SmtpConfiguration{host='"
        + host
        + "', port="
        + port
        + ", username='"
        + username
        + "', password=***, fromEmail='"
        + fromEmail
        + "', fromName='"
        + fromName
        + "', tlsEnabled="
        + tlsEnabled
        + "}";
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
