package com.mailSender;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mail")
public class MailAppProperties {

  private String from = "";
  private String subject = "";
  private String excelFilePath = "";
  private String bodyFilePath = "";
  private String attachmentPath = "";
  private boolean batchEnabled = false;
  private boolean dryRun = true;
  private boolean html = false;
  private String sentLogPath = "sent-addresses.txt";
  private long sendDelayMs = 1000;

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getExcelFilePath() {
    return excelFilePath;
  }

  public void setExcelFilePath(String excelFilePath) {
    this.excelFilePath = excelFilePath;
  }

  public String getBodyFilePath() {
    return bodyFilePath;
  }

  public void setBodyFilePath(String bodyFilePath) {
    this.bodyFilePath = bodyFilePath;
  }

  public String getAttachmentPath() {
    return attachmentPath;
  }

  public void setAttachmentPath(String attachmentPath) {
    this.attachmentPath = attachmentPath;
  }

  public boolean isBatchEnabled() {
    return batchEnabled;
  }

  public void setBatchEnabled(boolean batchEnabled) {
    this.batchEnabled = batchEnabled;
  }

  public boolean isDryRun() {
    return dryRun;
  }

  public void setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
  }

  public boolean isHtml() {
    return html;
  }

  public void setHtml(boolean html) {
    this.html = html;
  }

  public String getSentLogPath() {
    return sentLogPath;
  }

  public void setSentLogPath(String sentLogPath) {
    this.sentLogPath = sentLogPath;
  }

  public long getSendDelayMs() {
    return sendDelayMs;
  }

  public void setSendDelayMs(long sendDelayMs) {
    this.sendDelayMs = sendDelayMs;
  }
}
