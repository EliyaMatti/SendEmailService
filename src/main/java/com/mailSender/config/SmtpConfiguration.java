package com.mailSender.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.stereotype.Component;

/**
 * SMTP connection and envelope settings from {@code spring.mail.*} plus {@code mail.from} /
 * {@code mail.from-name}. The password is held in memory only and is never included in {@link
 * #toString()}.
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
  private final boolean authEnabled;

  @Autowired
  public SmtpConfiguration(MailProperties mailProperties, MailAppProperties mailAppProperties) {
    this(
        emptyIfNull(mailProperties.getHost()),
        mailProperties.getPort() == null ? 0 : mailProperties.getPort(),
        emptyIfNull(mailProperties.getUsername()),
        emptyIfNull(mailProperties.getPassword()),
        emptyIfNull(mailAppProperties.getFrom()),
        emptyIfNull(mailAppProperties.getFromName()),
        flag(mailProperties, "mail.smtp.starttls.enable", false),
        flag(mailProperties, "mail.smtp.auth", false));
  }

  public SmtpConfiguration(
      String host,
      int port,
      String username,
      String password,
      String fromEmail,
      String fromName,
      boolean tlsEnabled) {
    this(host, port, username, password, fromEmail, fromName, tlsEnabled, true);
  }

  public SmtpConfiguration(
      String host,
      int port,
      String username,
      String password,
      String fromEmail,
      String fromName,
      boolean tlsEnabled,
      boolean authEnabled) {
    this.host = emptyIfNull(host);
    this.port = port;
    this.username = emptyIfNull(username);
    this.password = emptyIfNull(password);
    this.fromEmail = emptyIfNull(fromEmail);
    this.fromName = emptyIfNull(fromName);
    this.tlsEnabled = tlsEnabled;
    this.authEnabled = authEnabled;
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

  public boolean isAuthEnabled() {
    return authEnabled;
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
        + ", authEnabled="
        + authEnabled
        + "}";
  }

  private static boolean flag(MailProperties mailProperties, String key, boolean defaultValue) {
    if (mailProperties.getProperties() == null) {
      return defaultValue;
    }
    String raw = mailProperties.getProperties().get(key);
    if (raw == null || raw.isBlank()) {
      return defaultValue;
    }
    return Boolean.parseBoolean(raw);
  }

  private static String emptyIfNull(String value) {
    return value == null ? "" : value;
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
