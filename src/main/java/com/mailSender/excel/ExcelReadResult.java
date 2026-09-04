package com.mailSender.excel;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ExcelReadResult {

  private final List<Contact> contacts;
  private final int totalRows;
  private final int valid;
  private final int invalid;
  private final int duplicates;
  private final Set<String> placeholderKeys;

  public ExcelReadResult(
      List<Contact> contacts,
      int totalRows,
      int valid,
      int invalid,
      int duplicates,
      Set<String> placeholderKeys) {
    this.contacts = List.copyOf(contacts);
    this.totalRows = totalRows;
    this.valid = valid;
    this.invalid = invalid;
    this.duplicates = duplicates;
    this.placeholderKeys =
        Collections.unmodifiableSet(new LinkedHashSet<>(placeholderKeys));
  }

  public List<Contact> getContacts() {
    return contacts;
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

  public Set<String> getPlaceholderKeys() {
    return placeholderKeys;
  }

  public String summary() {
    return "Total rows: "
        + totalRows
        + "\nValid: "
        + valid
        + "\nInvalid: "
        + invalid
        + "\nDuplicates: "
        + duplicates;
  }
}
