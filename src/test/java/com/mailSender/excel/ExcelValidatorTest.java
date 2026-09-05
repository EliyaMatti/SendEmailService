package com.mailSender.excel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExcelValidatorTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("Missing Excel file")
  void missingFileFails() {
    Path missing = tempDir.resolve("no-such.xlsx");
    ExcelProcessingException ex =
        assertThrows(
            ExcelProcessingException.class, () -> ExcelValidator.requireXlsxFile(missing.toString()));
    assertTrue(ex.getMessage().contains("not found or cannot be read"));
  }

  @Test
  @DisplayName("Non-.xlsx file")
  void nonXlsxFails() throws Exception {
    Path csv = tempDir.resolve("contacts.csv");
    Files.writeString(csv, "email,name\n");
    ExcelProcessingException ex =
        assertThrows(
            ExcelProcessingException.class, () -> ExcelValidator.requireXlsxFile(csv.toString()));
    assertTrue(ex.getMessage().contains("not a .xlsx workbook"));
  }

  @Test
  @DisplayName("Empty sheet")
  void emptySheetFails() throws Exception {
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet();
      ExcelProcessingException ex =
          assertThrows(
              ExcelProcessingException.class,
              () -> ExcelValidator.requireSheetNotEmpty(sheet, "empty.xlsx"));
      assertTrue(ex.getMessage().contains("contains no rows"));
    }
  }

  @Test
  @DisplayName("Empty row")
  void emptyRowDetection() {
    assertTrue(ExcelValidator.isEmptyRow("", "", Map.of()));
    assertTrue(ExcelValidator.isEmptyRow("  ", "  ", Map.of("company", "  ")));
    assertFalse(ExcelValidator.isEmptyRow("a@example.com", "", Map.of()));
    assertFalse(ExcelValidator.isEmptyRow("", "Ada", Map.of()));
    assertFalse(ExcelValidator.isEmptyRow("", "", Map.of("company", "Acme")));
    assertTrue(ExcelValidator.isEmptyRow("", "", null));
  }

  @Test
  @DisplayName("Invalid email")
  void emailMustContainAtSign() {
    assertFalse(ExcelValidator.isValidEmail(null));
    assertFalse(ExcelValidator.isValidEmail("not-an-email"));
    assertTrue(ExcelValidator.isValidEmail("ada@example.com"));
  }

  @Test
  @DisplayName("Duplicate emails")
  void duplicateUsesNormalizedEmail() {
    Set<String> seen = new LinkedHashSet<>();
    seen.add("ada@example.com");
    assertTrue(ExcelValidator.isDuplicate("Ada@Example.com", seen));
    assertFalse(ExcelValidator.isDuplicate("bob@example.com", seen));
    assertEquals("", ExcelValidator.normalizeEmail(null));
    assertEquals("ada@example.com", ExcelValidator.normalizeEmail("Ada@Example.com"));
  }

  @Test
  void readableXlsxPassesFileCheck() throws Exception {
    Path excel = tempDir.resolve("ok.xlsx");
    try (Workbook workbook = new XSSFWorkbook()) {
      workbook.createSheet().createRow(0).createCell(0).setCellValue("Email");
      try (var out = Files.newOutputStream(excel)) {
        workbook.write(out);
      }
    }
    assertEquals(excel, ExcelValidator.requireXlsxFile(excel.toString()));
  }
}
