package com.mailSender.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "excelmail")
public class ExcelmailProperties {

  private final Security security = new Security();
  private final Crypto crypto = new Crypto();
  private final Auth auth = new Auth();
  private final Worker worker = new Worker();
  private final Limits limits = new Limits();
  private final Upload upload = new Upload();

  public Security getSecurity() {
    return security;
  }

  public Crypto getCrypto() {
    return crypto;
  }

  public Auth getAuth() {
    return auth;
  }

  public Worker getWorker() {
    return worker;
  }

  public Limits getLimits() {
    return limits;
  }

  public Upload getUpload() {
    return upload;
  }

  public static class Security {
    private String jwtSecret = "";
    private long jwtExpirationMs = 86400000L;

    public String getJwtSecret() {
      return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
      this.jwtSecret = jwtSecret;
    }

    public long getJwtExpirationMs() {
      return jwtExpirationMs;
    }

    public void setJwtExpirationMs(long jwtExpirationMs) {
      this.jwtExpirationMs = jwtExpirationMs;
    }
  }

  public static class Crypto {
    private String key = "";
    private int keyVersion = 1;

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public int getKeyVersion() {
      return keyVersion;
    }

    public void setKeyVersion(int keyVersion) {
      this.keyVersion = keyVersion;
    }
  }

  public static class Auth {
    private int rateLimitPerMinute = 30;

    public int getRateLimitPerMinute() {
      return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
      this.rateLimitPerMinute = rateLimitPerMinute;
    }
  }

  public static class Worker {
    private boolean enabled = true;
    private long pollMs = 2000;
    private int maxAttempts = 3;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public long getPollMs() {
      return pollMs;
    }

    public void setPollMs(long pollMs) {
      this.pollMs = pollMs;
    }

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }
  }

  public static class Limits {
    private int maxRecipientsPerCampaign = 10000;
    private int maxSendsPerMinute = 30;
    private int maxDailySends = 500;

    public int getMaxRecipientsPerCampaign() {
      return maxRecipientsPerCampaign;
    }

    public void setMaxRecipientsPerCampaign(int maxRecipientsPerCampaign) {
      this.maxRecipientsPerCampaign = maxRecipientsPerCampaign;
    }

    public int getMaxSendsPerMinute() {
      return maxSendsPerMinute;
    }

    public void setMaxSendsPerMinute(int maxSendsPerMinute) {
      this.maxSendsPerMinute = maxSendsPerMinute;
    }

    public int getMaxDailySends() {
      return maxDailySends;
    }

    public void setMaxDailySends(int maxDailySends) {
      this.maxDailySends = maxDailySends;
    }
  }

  public static class Upload {
    private long maxFileBytes = 5_000_000;

    public long getMaxFileBytes() {
      return maxFileBytes;
    }

    public void setMaxFileBytes(long maxFileBytes) {
      this.maxFileBytes = maxFileBytes;
    }
  }
}
