package com.mailSender;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadFromExcel {

  private static final Logger log = LoggerFactory.getLogger(ReadFromExcel.class);

  public static List<EmailRecipient> readEmailsAndNamesFromExcel(String filePath) {
    Path path = Path.of(filePath);
    if (!Files.isRegularFile(path)) {
      throw new IllegalStateException("Cannot read Excel file: " + filePath);
    }

    List<EmailRecipient> recipients = new ArrayList<>();
    try (InputStream in = Files.newInputStream(path);
        Workbook workbook = new XSSFWorkbook(in)) {
      Sheet sheet = workbook.getSheetAt(0);
      DataFormatter formatter = new DataFormatter();
      FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

      Row firstRow = sheet.getRow(sheet.getFirstRowNum());
      ColumnMap columns = ColumnMap.fromFirstRow(firstRow, formatter, evaluator);

      int startRow = columns.headerRow() ? sheet.getFirstRowNum() + 1 : sheet.getFirstRowNum();
      for (int r = startRow; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        if (row == null) {
          continue;
        }
        int rowNumber = r + 1;
        String email = columns.email(row, formatter, evaluator);
        String name = columns.name(row, formatter, evaluator);
        if (email.isBlank()) {
          log.warn("Skipping row {}: blank email", rowNumber);
          continue;
        }
        if (!email.contains("@")) {
          log.warn("Skipping row {}: invalid email '{}'", rowNumber, email);
          continue;
        }
        recipients.add(new EmailRecipient(email, name, columns.extras(row, formatter, evaluator)));
      }
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Cannot read Excel file: " + filePath, e);
    }
    return recipients;
  }

  private static String cellValue(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
    if (cell == null) {
      return "";
    }
    return formatter.formatCellValue(cell, evaluator).trim();
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

    static ColumnMap fromFirstRow(
        Row firstRow, DataFormatter formatter, FormulaEvaluator evaluator) {
      if (firstRow == null) {
        return new ColumnMap(0, 1, Map.of(), false);
      }
      Map<Integer, String> extras = new LinkedHashMap<>();
      int emailIndex = -1;
      int nameIndex = -1;
      short last = firstRow.getLastCellNum();
      for (int i = 0; i < last; i++) {
        String header = cellValue(firstRow.getCell(i), formatter, evaluator);
        String key = sanitizeKey(header);
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
      if (emailIndex >= 0) {
        String colA = cellValue(firstRow.getCell(0), formatter, evaluator);
        if (!colA.contains("@")) {
          throw new IllegalStateException("Excel header row must include email and name columns");
        }
      }
      return new ColumnMap(0, 1, Map.of(), false);
    }

    boolean headerRow() {
      return headerRow;
    }

    String email(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
      return cellValue(row.getCell(emailIndex), formatter, evaluator);
    }

    String name(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
      return cellValue(row.getCell(nameIndex), formatter, evaluator);
    }

    Map<String, String> extras(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
      if (extraIndexes.isEmpty()) {
        return Map.of();
      }
      Map<String, String> values = new LinkedHashMap<>();
      extraIndexes.forEach(
          (index, key) -> values.put(key, cellValue(row.getCell(index), formatter, evaluator)));
      return values;
    }
  }
}
