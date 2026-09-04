package com.mailSender.excel;

/** Excel file cannot be read or does not meet required structure. */
public class ExcelProcessingException extends RuntimeException {

  public ExcelProcessingException(String message) {
    super(message);
  }

  public ExcelProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}
