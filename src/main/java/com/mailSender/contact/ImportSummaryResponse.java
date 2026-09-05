package com.mailSender.contact;

import java.util.List;

public class ImportSummaryResponse {

  private int totalRows;
  private int valid;
  private int invalid;
  private int duplicates;
  private List<String> errors;
  private ContactListResponse list;

  public ImportSummaryResponse(
      int totalRows,
      int valid,
      int invalid,
      int duplicates,
      List<String> errors,
      ContactListResponse list) {
    this.totalRows = totalRows;
    this.valid = valid;
    this.invalid = invalid;
    this.duplicates = duplicates;
    this.errors = errors;
    this.list = list;
  }

  public int getTotalRows() {
    return totalRows;
  }

  public int getValid() {
    return valid;
  }

  public int getInvalid() {
    return invalid;
  }

  public int getDuplicates() {
    return duplicates;
  }

  public List<String> getErrors() {
    return errors;
  }

  public ContactListResponse getList() {
    return list;
  }
}
