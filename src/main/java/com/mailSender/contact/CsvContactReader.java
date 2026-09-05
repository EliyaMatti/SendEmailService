package com.mailSender.contact;

import com.mailSender.excel.Contact;
import com.mailSender.excel.ExcelReadResult;
import com.mailSender.excel.ExcelValidator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CSV importer for the API. Does not change {@code ExcelReader} .xlsx rules; it applies the same
 * skip/duplicate policy to delimited files.
 */
public final class CsvContactReader {

  private static final Logger log = LoggerFactory.getLogger(CsvContactReader.class);
  private static final int CONSECUTIVE_EMPTY_ROW_LIMIT = 10;

  private CsvContactReader() {}

  public static ExcelReadResult read(InputStream input) throws IOException {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
      CSVParser parser = CSVFormat.DEFAULT.parse(reader);
      List<CSVRecord> records = parser.getRecords();
      if (records.isEmpty()) {
        throw new IllegalArgumentException("Unable to process the CSV file because it contains no rows.");
      }

      ColumnMap columns = ColumnMap.fromFirstRow(records.get(0));
      Set<String> placeholderKeys = new LinkedHashSet<>();
      placeholderKeys.add("email");
      placeholderKeys.add("name");
      placeholderKeys.addAll(columns.extraKeys());

      List<Contact> contacts = new ArrayList<>();
      List<String> rowErrors = new ArrayList<>();
      Set<String> seenEmails = new LinkedHashSet<>();
      int totalRows = 0;
      int invalid = 0;
      int duplicates = 0;
      int consecutiveEmptyRows = 0;
      int start = columns.headerRow() ? 1 : 0;
      for (int i = start; i < records.size(); i++) {
        totalRows++;
        CSVRecord record = records.get(i);
        int rowNumber = i + 1;
        String email = columns.email(record);
        String name = columns.name(record);
        Map<String, String> extras = columns.extras(record);
        if (ExcelValidator.isEmptyRow(email, name, extras)) {
          consecutiveEmptyRows++;
          invalid++;
          rowErrors.add("Row " + rowNumber + ": Missing email");
          log.warn("Skipping row {}: empty row", rowNumber);
          if (consecutiveEmptyRows >= CONSECUTIVE_EMPTY_ROW_LIMIT) {
            break;
          }
          continue;
        }
        consecutiveEmptyRows = 0;
        if (email.isBlank()) {
          invalid++;
          rowErrors.add("Row " + rowNumber + ": Missing email");
          continue;
        }
        if (!ExcelValidator.isValidEmail(email)) {
          invalid++;
          rowErrors.add("Row " + rowNumber + ": Invalid email format");
          continue;
        }
        if (ExcelValidator.isDuplicate(email, seenEmails)) {
          duplicates++;
          rowErrors.add("Row " + rowNumber + ": Duplicate email");
          continue;
        }
        seenEmails.add(ExcelValidator.normalizeEmail(email));
        contacts.add(new Contact(email, name, extras));
      }
      return new ExcelReadResult(
          contacts, totalRows, contacts.size(), invalid, duplicates, placeholderKeys, rowErrors);
    }
  }

  private static String sanitizeKey(String header) {
    StringBuilder key = new StringBuilder();
    for (int i = 0; i < header.length(); i++) {
      char c = header.charAt(i);
      if (Character.isLetterOrDigit(c) || c == '_') {
        key.append(Character.toLowerCase(c));
      }
    }
    return key.toString();
  }

  private static boolean isEmailHeader(String key) {
    return "email".equals(key) || "e_mail".equals(key) || "mail".equals(key);
  }

  private static boolean isNameHeader(String key) {
    return "name".equals(key) || "fullname".equals(key) || "full_name".equals(key);
  }

  private static final class ColumnMap {
    private final int emailIndex;
    private final int nameIndex;
    private final Map<Integer, String> extraIndexes;
    private final boolean headerRow;

    private ColumnMap(
        int emailIndex, int nameIndex, Map<Integer, String> extraIndexes, boolean headerRow) {
      this.emailIndex = emailIndex;
      this.nameIndex = nameIndex;
      this.extraIndexes = extraIndexes;
      this.headerRow = headerRow;
    }

    static ColumnMap fromFirstRow(CSVRecord firstRow) {
      Map<Integer, String> extras = new LinkedHashMap<>();
      int emailIndex = -1;
      int nameIndex = -1;
      for (int i = 0; i < firstRow.size(); i++) {
        String key = sanitizeKey(firstRow.get(i).trim());
        if (key.isEmpty()) {
          continue;
        }
        if (isEmailHeader(key) && emailIndex < 0) {
          emailIndex = i;
        } else if (isNameHeader(key) && nameIndex < 0) {
          nameIndex = i;
        } else {
          extras.put(i, key);
        }
      }
      boolean bothHeaders = emailIndex >= 0 && nameIndex >= 0;
      if (bothHeaders) {
        return new ColumnMap(emailIndex, nameIndex, extras, true);
      }
      String colA = firstRow.size() > 0 ? firstRow.get(0).trim() : "";
      boolean firstCellLooksLikeEmail = colA.contains("@");
      if (emailIndex >= 0 && nameIndex < 0 && !firstCellLooksLikeEmail) {
        throw new IllegalArgumentException(
            "Unable to process the CSV file because the Name column was not found.");
      }
      if (nameIndex >= 0 && emailIndex < 0 && !firstCellLooksLikeEmail) {
        throw new IllegalArgumentException(
            "Unable to process the CSV file because the Email column was not found.");
      }
      return new ColumnMap(0, 1, Map.of(), false);
    }

    boolean headerRow() {
      return headerRow;
    }

    String email(CSVRecord row) {
      return cell(row, emailIndex);
    }

    String name(CSVRecord row) {
      return cell(row, nameIndex);
    }

    Map<String, String> extras(CSVRecord row) {
      if (extraIndexes.isEmpty()) {
        return Map.of();
      }
      Map<String, String> values = new LinkedHashMap<>();
      extraIndexes.forEach((index, key) -> values.put(key, cell(row, index)));
      return values;
    }

    Set<String> extraKeys() {
      return new LinkedHashSet<>(extraIndexes.values());
    }

    private static String cell(CSVRecord row, int index) {
      if (index < 0 || index >= row.size()) {
        return "";
      }
      return row.get(index) == null ? "" : row.get(index).trim();
    }
  }
}
