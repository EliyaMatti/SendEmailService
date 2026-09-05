package com.mailSender.smtpaccount;

public class SmtpTestResponse {

  private boolean success;
  private String message;

  public SmtpTestResponse(boolean success, String message) {
    this.success = success;
    this.message = message;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getMessage() {
    return message;
  }
}
