package com.mailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReadFromExcelTest {

  @TempDir Path tempDir;

  @Test
  void missingExcelFileFailsLoudly() {
    Path missing = tempDir.resolve("missing.xlsx");
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> ReadFromExcel.readEmailsAndNamesFromExcel(missing.toString()));
    assertTrue(ex.getMessage().contains("Cannot read Excel file"));
  }

  @Test
  void corruptOrNonXlsxFileFailsLoudly() throws Exception {
    Path notExcel = tempDir.resolve("not-excel.xlsx");
    java.nio.file.Files.writeString(notExcel, "this is not an xlsx file");
    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> ReadFromExcel.readEmailsAndNamesFromExcel(notExcel.toString()));
    assertTrue(ex.getMessage().contains("Cannot read Excel file"));
  }

  @Test
  void skipsHeaderTrimsInvalidAndReadsFormulaCells() throws Exception {
    Path excel = tempDir.resolve("recipients.xlsx");
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet();
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("Email");
      header.createCell(1).setCellValue("Name");

      Row valid = sheet.createRow(1);
      valid.createCell(0).setCellValue("  a@example.com  ");
      valid.createCell(1).setCellValue("  Ada  ");

      Row blank = sheet.createRow(2);
      blank.createCell(0).setCellValue("   ");
      blank.createCell(1).setCellValue("Blank");

      Row invalid = sheet.createRow(3);
      invalid.createCell(0).setCellValue("not-an-email");
      invalid.createCell(1).setCellValue("Bad");

      Row formula = sheet.createRow(4);
      formula.createCell(0).setCellFormula("\"formula@example.com\"");
      formula.createCell(1).setCellFormula("\"Formula\"");

      try (var out = java.nio.file.Files.newOutputStream(excel)) {
        workbook.write(out);
      }
    }

    List<EmailRecipient> recipients = ReadFromExcel.readEmailsAndNamesFromExcel(excel.toString());
    assertEquals(2, recipients.size());
    assertEquals("a@example.com", recipients.get(0).getEmail());
    assertEquals("Ada", recipients.get(0).getName());
    assertEquals("formula@example.com", recipients.get(1).getEmail());
    assertEquals("Formula", recipients.get(1).getName());
  }

  @Test
  void extraHeaderColumnsBecomePlaceholders() throws Exception {
    Path excel = tempDir.resolve("extra.xlsx");
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet();
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("email");
      header.createCell(1).setCellValue("name");
      header.createCell(2).setCellValue("Company");
      Row row = sheet.createRow(1);
      row.createCell(0).setCellValue("a@example.com");
      row.createCell(1).setCellValue("Ada");
      row.createCell(2).setCellValue("Acme");
      try (var out = java.nio.file.Files.newOutputStream(excel)) {
        workbook.write(out);
      }
    }

    List<EmailRecipient> recipients = ReadFromExcel.readEmailsAndNamesFromExcel(excel.toString());
    assertEquals(1, recipients.size());
    assertEquals("Acme", recipients.get(0).getPlaceholders().get("company"));
  }

  @Test
  void headerlessSheetUsesColumnAEmailAndColumnBName() throws Exception {
    Path excel = tempDir.resolve("ab.xlsx");
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet();
      Row row = sheet.createRow(0);
      row.createCell(0).setCellValue("a@example.com");
      row.createCell(1).setCellValue("Ada");
      try (var out = java.nio.file.Files.newOutputStream(excel)) {
        workbook.write(out);
      }
    }

    List<EmailRecipient> recipients = ReadFromExcel.readEmailsAndNamesFromExcel(excel.toString());
    assertEquals(1, recipients.size());
    assertEquals("a@example.com", recipients.get(0).getEmail());
    assertEquals("Ada", recipients.get(0).getName());
  }

  @Test
  void headerlessSheetUsesColumnAEmailEvenIfFirstNameLooksLikeAHeader() throws Exception {
    Path excel = tempDir.resolve("headerless.xlsx");
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet();
      Row first = sheet.createRow(0);
      first.createCell(0).setCellValue("a@example.com");
      first.createCell(1).setCellValue("Name");
      Row second = sheet.createRow(1);
      second.createCell(0).setCellValue("b@example.com");
      second.createCell(1).setCellValue("Email");
      Row third = sheet.createRow(2);
      third.createCell(0).setCellValue("c@example.com");
      third.createCell(1).setCellValue("Full Name");
      try (var out = java.nio.file.Files.newOutputStream(excel)) {
        workbook.write(out);
      }
    }

    List<EmailRecipient> recipients = ReadFromExcel.readEmailsAndNamesFromExcel(excel.toString());
    assertEquals(3, recipients.size());
    assertEquals("a@example.com", recipients.get(0).getEmail());
    assertEquals("Name", recipients.get(0).getName());
    assertEquals("b@example.com", recipients.get(1).getEmail());
    assertEquals("Email", recipients.get(1).getName());
    assertEquals("c@example.com", recipients.get(2).getEmail());
    assertEquals("Full Name", recipients.get(2).getName());
  }

  @Test
  void headerRowWithEmailButNoNameFailsLoudly() throws Exception {
    Path excel = tempDir.resolve("email-only-header.xlsx");
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet();
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("email");
      header.createCell(1).setCellValue("company");
      Row row = sheet.createRow(1);
      row.createCell(0).setCellValue("a@example.com");
      row.createCell(1).setCellValue("Acme");
      try (var out = java.nio.file.Files.newOutputStream(excel)) {
        workbook.write(out);
      }
    }

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> ReadFromExcel.readEmailsAndNamesFromExcel(excel.toString()));
    assertTrue(ex.getMessage().contains("email and name"));
  }
}
