package com.mailSender.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

  private boolean success;
  private T data;
  private ApiError error;

  public static <T> ApiResponse<T> ok(T data) {
    ApiResponse<T> response = new ApiResponse<>();
    response.success = true;
    response.data = data;
    return response;
  }

  public static ApiResponse<Void> fail(String code, String message) {
    ApiResponse<Void> response = new ApiResponse<>();
    response.success = false;
    response.error = new ApiError(code, message);
    return response;
  }

  public boolean isSuccess() {
    return success;
  }

  public T getData() {
    return data;
  }

  public ApiError getError() {
    return error;
  }

  public static class ApiError {
    private String code;
    private String message;

    public ApiError() {}

    public ApiError(String code, String message) {
      this.code = code;
      this.message = message;
    }

    public String getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }
  }
}
