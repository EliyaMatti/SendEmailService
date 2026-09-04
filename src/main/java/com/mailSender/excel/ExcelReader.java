package com.mailSender.excel;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelReader {

  private static final Logger log = LoggerFactory.getLogger(ExcelReader.class);

  public static ExcelReadResult read(String filePath) {
    Path path = ExcelValidator.requireXlsxFile(filePath);

    try (InputStream in = Files.newInputStream(path);
        Workbook workbook = new XSSFWorkbook(in)) {
      Sheet sheet = workbook.getSheetAt(0);
      ExcelValidator.requireSheetNotEmpty(sheet, filePath);
      DataFormatter formatter = new DataFormatter();
      FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

      Row firstRow = sheet.getRow(sheet.getFirstRowNum());
      ColumnMap columns = ColumnMap.fromFirstRow(firstRow, formatter, evaluator);

      Set<String> placeholderKeys = new LinkedHashSet<>();
      placeholderKeys.add("email");
      placeholderKeys.add("name");
      placeholderKeys.addAll(columns.extraKeys());

      List<Contact> contacts = new ArrayList<>();
      Set<String> seenEmails = new LinkedHashSet<>();
      int totalRows = 0;
      int invalid = 0;
      int duplicates = 0;

      int startRow = columns.headerRow() ? sheet.getFirstRowNum() + 1 : sheet.getFirstRowNum();
      for (int r = startRow; r <= sheet.getLastRowNum(); r++) {
        totalRows++;
        Row row = sheet.getRow(r);
        int rowNumber = r + 1;
        if (row == null) {
          invalid++;
          log.warn("Skipping row {}: empty row", rowNumber);
          continue;
        }
        String email = columns.email(row, formatter, evaluator);
        String name = columns.name(row, formatter, evaluator);
        Map<String, String> extras = columns.extras(row, formatter, evaluator);
        if (ExcelValidator.isEmptyRow(email, name, extras)) {
          invalid++;
          log.warn("Skipping row {}: empty row", rowNumber);
          continue;
        }
        if (email.isBlank()) {
          invalid++;
          log.warn("Skipping row {}: blank email", rowNumber);
          continue;
        }
        if (!ExcelValidator.isValidEmail(email)) {
          invalid++;
          log.warn("Skipping row {}: invalid email '{}'", rowNumber, email);
          continue;
        }
        if (ExcelValidator.isDuplicate(email, seenEmails)) {
          duplicates++;
          log.warn("Skipping row {}: duplicate email '{}'", rowNumber, email);
          continue;
        }
        seenEmails.add(ExcelValidator.normalizeEmail(email));
        contacts.add(new Contact(email, name, extras));
      }

      ExcelReadResult result =
          new ExcelReadResult(
              contacts, totalRows, contacts.size(), invalid, duplicates, placeholderKeys);
      log.info("Excel file loaded: {}", path.getFileName());
      log.info("Total rows: {}", result.getTotalRows());
      log.info("Valid: {}", result.getValid());
      log.info("Invalid: {}", result.getInvalid());
      log.info("Duplicates: {}", result.getDuplicates());
      return result;
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Cannot read Excel file: " + filePath, e);
    }
  }


  static String cellValue(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
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

  static final class ColumnMap {
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

    Set<String> extraKeys() {
      return new LinkedHashSet<>(extraIndexes.values());
    }
  }
}
