package com.mailSender.common.exception;

public class ApiException extends RuntimeException {

  private final String code;
  private final int httpStatus;

  public ApiException(String code, String message) {
    this(code, message, 400);
  }

  public ApiException(String code, String message, int httpStatus) {
    super(message);
    this.code = code;
    this.httpStatus = httpStatus;
  }

  public String getCode() {
    return code;
  }

  public int getHttpStatus() {
    return httpStatus;
  }
}
