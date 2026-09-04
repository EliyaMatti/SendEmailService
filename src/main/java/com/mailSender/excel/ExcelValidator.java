package com.mailSender.excel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Sheet;

public final class ExcelValidator {

  private ExcelValidator() {}

  public static Path requireXlsxFile(String filePath) {
    Path path = Path.of(filePath);
    if (!Files.isRegularFile(path)) {
      throw new IllegalStateException("Cannot read Excel file: " + filePath);
    }
    String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
    if (!fileName.endsWith(".xlsx")) {
      throw new IllegalStateException(
          "Unsupported Excel file type (expected .xlsx): " + filePath);
    }
    return path;
  }

  public static void requireSheetNotEmpty(Sheet sheet, String filePath) {
    if (sheet.getPhysicalNumberOfRows() == 0) {
      throw new IllegalStateException("Excel file is empty: " + filePath);
    }
  }

  public static boolean isEmptyRow(String email, String name, Map<String, String> extras) {
    if (!isBlank(email) || !isBlank(name)) {
      return false;
    }
    if (extras == null || extras.isEmpty()) {
      return true;
    }
    for (String value : extras.values()) {
      if (!isBlank(value)) {
        return false;
      }
    }
    return true;
  }

  public static boolean isValidEmail(String email) {
    return email != null && email.contains("@");
  }

  public static String normalizeEmail(String email) {
    return email == null ? "" : email.toLowerCase(Locale.ROOT);
  }

  public static boolean isDuplicate(String email, Set<String> seenNormalized) {
    return seenNormalized.contains(normalizeEmail(email));
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
