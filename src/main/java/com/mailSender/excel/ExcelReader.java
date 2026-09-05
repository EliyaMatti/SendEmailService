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
  private static final int CONSECUTIVE_EMPTY_ROW_LIMIT = 10;

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
      List<String> rowErrors = new ArrayList<>();
      Set<String> seenEmails = new LinkedHashSet<>();
      int totalRows = 0;
      int invalid = 0;
      int duplicates = 0;

      int consecutiveEmptyRows = 0;
      int startRow = columns.headerRow() ? sheet.getFirstRowNum() + 1 : sheet.getFirstRowNum();
      for (int rowIndex = startRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        totalRows++;
        Row row = sheet.getRow(rowIndex);
        int rowNumber = rowIndex + 1;
        if (row == null) {
          consecutiveEmptyRows++;
          invalid++;
          rowErrors.add("Row " + rowNumber + ": Missing email");
          log.warn("Skipping row {}: empty row", rowNumber);
          if (stopAfterConsecutiveEmptyRows(consecutiveEmptyRows, rowNumber)) {
            break;
          }
          continue;
        }
        String email = columns.email(row, formatter, evaluator);
        String name = columns.name(row, formatter, evaluator);
        Map<String, String> extras = columns.extras(row, formatter, evaluator);
        if (ExcelValidator.isEmptyRow(email, name, extras)) {
          consecutiveEmptyRows++;
          invalid++;
          rowErrors.add("Row " + rowNumber + ": Missing email");
          log.warn("Skipping row {}: empty row", rowNumber);
          if (stopAfterConsecutiveEmptyRows(consecutiveEmptyRows, rowNumber)) {
            break;
          }
          continue;
        }
        consecutiveEmptyRows = 0;
        if (email.isBlank()) {
          invalid++;
          rowErrors.add("Row " + rowNumber + ": Missing email");
          log.warn("Skipping row {}: blank email", rowNumber);
          continue;
        }
        if (!ExcelValidator.isValidEmail(email)) {
          invalid++;
          rowErrors.add("Row " + rowNumber + ": Invalid email format");
          log.warn("Skipping row {}: invalid email '{}'", rowNumber, email);
          continue;
        }
        if (ExcelValidator.isDuplicate(email, seenEmails)) {
          duplicates++;
          rowErrors.add("Row " + rowNumber + ": Duplicate email");
          log.warn("Skipping row {}: duplicate email '{}'", rowNumber, email);
          continue;
        }
        seenEmails.add(ExcelValidator.normalizeEmail(email));
        contacts.add(new Contact(email, name, extras));
      }

      ExcelReadResult result =
          new ExcelReadResult(
              contacts, totalRows, contacts.size(), invalid, duplicates, placeholderKeys, rowErrors);
      log.info("Excel file loaded: {}", path.getFileName());
      log.info("Total rows: {}", result.getTotalRows());
      log.info("Valid contacts: {}", result.getValid());
      log.info("Invalid contacts: {}", result.getInvalid());
      log.info("Duplicates: {}", result.getDuplicates());
      return result;
    } catch (ExcelProcessingException e) {
      throw e;
    } catch (Exception e) {
      log.debug("Excel workbook could not be opened: {}", filePath, e);
      throw new ExcelProcessingException(
          "Unable to process the Excel file. Check that the path points to a valid .xlsx workbook.",
          e);
    }
  }


  private static boolean stopAfterConsecutiveEmptyRows(int consecutiveEmptyRows, int rowNumber) {
    if (consecutiveEmptyRows < CONSECUTIVE_EMPTY_ROW_LIMIT) {
      return false;
    }
    log.info(
        "Stopped reading after {} consecutive empty rows at row {}",
        CONSECUTIVE_EMPTY_ROW_LIMIT,
        rowNumber);
    return true;
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
      String colA = cellValue(firstRow.getCell(0), formatter, evaluator);
      boolean firstCellLooksLikeEmail = colA.contains("@");
      if (emailIndex >= 0 && nameIndex < 0 && !firstCellLooksLikeEmail) {
        throw new ExcelProcessingException(
            "Unable to process the Excel file because the Name column was not found.");
      }
      if (nameIndex >= 0 && emailIndex < 0 && !firstCellLooksLikeEmail) {
        throw new ExcelProcessingException(
            "Unable to process the Excel file because the Email column was not found.");
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
