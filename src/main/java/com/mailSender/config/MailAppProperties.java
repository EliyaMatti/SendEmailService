package com.mailSender.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Central {@code mail.*} settings: input files, sending (delay, dry-run, batch, test-send), and
 * envelope from. SMTP host/port/credentials live in Spring {@code spring.mail.*} and {@link
 * SmtpConfiguration}.
 */
@ConfigurationProperties(prefix = "mail")
public class MailAppProperties {

  private String from = "";
  private String fromName = "";
  private String subject = "";
  private String excelFilePath = "";
  private String bodyFilePath = "";
  private String attachmentPath = "";
  private boolean batchEnabled = false;
  private boolean dryRun = true;
  private boolean html = false;
  private String sentLogPath = "sent-addresses.txt";
  private long sendDelayMs = 1000;
  private boolean testSendEnabled = false;
  private String testSendTo = "";

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getFromName() {
    return fromName;
  }

  public void setFromName(String fromName) {
    this.fromName = fromName;
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

  public boolean isTestSendEnabled() {
    return testSendEnabled;
  }

  public void setTestSendEnabled(boolean testSendEnabled) {
    this.testSendEnabled = testSendEnabled;
  }

  public String getTestSendTo() {
    return testSendTo;
  }

  public void setTestSendTo(String testSendTo) {
    this.testSendTo = testSendTo;
  }
}
