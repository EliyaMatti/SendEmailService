package com.mailSender.excel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class ExcelReaderTest {

  @TempDir Path tempDir;

  @Test
  void missingExcelFileFailsLoudly() {
    Path missing = tempDir.resolve("missing.xlsx");
    ExcelProcessingException ex =
        assertThrows(ExcelProcessingException.class, () -> ExcelReader.read(missing.toString()));
    assertTrue(ex.getMessage().contains("not found or cannot be read"));
  }

  @Test
  void corruptOrNonXlsxFileFailsLoudly() throws Exception {
    Path notExcel = tempDir.resolve("not-excel.xlsx");
    java.nio.file.Files.writeString(notExcel, "this is not an xlsx file");
    ExcelProcessingException ex =
        assertThrows(ExcelProcessingException.class, () -> ExcelReader.read(notExcel.toString()));
    assertTrue(ex.getMessage().contains("valid .xlsx workbook"));
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

    List<Contact> contacts = ExcelReader.read(excel.toString()).getContacts();
    assertEquals(2, contacts.size());
    assertEquals("a@example.com", contacts.get(0).getEmail());
    assertEquals("Ada", contacts.get(0).getName());
    assertEquals("formula@example.com", contacts.get(1).getEmail());
    assertEquals("Formula", contacts.get(1).getName());
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

    List<Contact> contacts = ExcelReader.read(excel.toString()).getContacts();
    assertEquals(1, contacts.size());
    assertEquals("Acme", contacts.get(0).getPlaceholders().get("company"));
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

    List<Contact> contacts = ExcelReader.read(excel.toString()).getContacts();
    assertEquals(1, contacts.size());
    assertEquals("a@example.com", contacts.get(0).getEmail());
    assertEquals("Ada", contacts.get(0).getName());
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

    List<Contact> contacts = ExcelReader.read(excel.toString()).getContacts();
    assertEquals(3, contacts.size());
    assertEquals("a@example.com", contacts.get(0).getEmail());
    assertEquals("Name", contacts.get(0).getName());
    assertEquals("b@example.com", contacts.get(1).getEmail());
    assertEquals("Email", contacts.get(1).getName());
    assertEquals("c@example.com", contacts.get(2).getEmail());
    assertEquals("Full Name", contacts.get(2).getName());
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

    ExcelProcessingException ex =
        assertThrows(ExcelProcessingException.class, () -> ExcelReader.read(excel.toString()));
    assertTrue(ex.getMessage().contains("the Name column was not found"));
  }

  @Test
  void headerRowWithNameButNoEmailFailsLoudly() throws Exception {
    Path excel = tempDir.resolve("name-only-header.xlsx");
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet();
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("name");
      header.createCell(1).setCellValue("company");
      Row row = sheet.createRow(1);
      row.createCell(0).setCellValue("Ada");
      row.createCell(1).setCellValue("Acme");
      try (var out = java.nio.file.Files.newOutputStream(excel)) {
        workbook.write(out);
      }
    }

    ExcelProcessingException ex =
        assertThrows(ExcelProcessingException.class, () -> ExcelReader.read(excel.toString()));
    assertTrue(ex.getMessage().contains("the Email column was not found"));
  }

  @Test
  void unsupportedFileTypeFailsBeforeParse() throws Exception {
    Path csv = tempDir.resolve("contacts.csv");
    java.nio.file.Files.writeString(csv, "email,name\na@example.com,Ada");
    ExcelProcessingException ex =
        assertThrows(ExcelProcessingException.class, () -> ExcelReader.read(csv.toString()));
    assertTrue(ex.getMessage().contains("not a .xlsx workbook"));
  }

  @Test
  void emptyWorkbookFailsLoudly() throws Exception {
    Path excel = tempDir.resolve("empty.xlsx");
    try (Workbook workbook = new XSSFWorkbook()) {
      workbook.createSheet();
      try (var out = java.nio.file.Files.newOutputStream(excel)) {
        workbook.write(out);
      }
    }
    ExcelProcessingException ex =
        assertThrows(ExcelProcessingException.class, () -> ExcelReader.read(excel.toString()));
    assertTrue(ex.getMessage().contains("contains no rows"));
  }

  @Test
  void reportsInvalidAndDuplicateCountsAndKeepsFirstContact() throws Exception {
    Path excel = tempDir.resolve("mixed.xlsx");
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet();
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("email");
      header.createCell(1).setCellValue("name");
      header.createCell(2).setCellValue("Company");
      Row valid = sheet.createRow(1);
      valid.createCell(0).setCellValue("a@example.com");
      valid.createCell(1).setCellValue("Ada");
      valid.createCell(2).setCellValue("Acme");
      Row dup = sheet.createRow(2);
      dup.createCell(0).setCellValue("A@Example.com");
      dup.createCell(1).setCellValue("Ada 2");
      Row blankEmail = sheet.createRow(3);
      blankEmail.createCell(0).setCellValue("  ");
      blankEmail.createCell(1).setCellValue("Nameless");
      Row invalid = sheet.createRow(4);
      invalid.createCell(0).setCellValue("no-at-sign");
      invalid.createCell(1).setCellValue("Bad");
      try (var out = java.nio.file.Files.newOutputStream(excel)) {
        workbook.write(out);
      }
    }

    ExcelReadResult result = ExcelReader.read(excel.toString());
    assertEquals(4, result.getTotalRows());
    assertEquals(1, result.getValid());
    assertEquals(2, result.getInvalid());
    assertEquals(1, result.getDuplicates());
    assertEquals(1, result.getContacts().size());
    assertEquals("a@example.com", result.getContacts().get(0).getEmail());
    assertTrue(result.getPlaceholderKeys().contains("company"));
    assertTrue(result.summary().contains("Total rows: 4"));
    assertTrue(result.summary().contains("Valid: 1"));
    assertTrue(result.summary().contains("Invalid: 2"));
    assertTrue(result.summary().contains("Duplicates: 1"));
  }

  @Test
  void logsLoadCountsWithoutPasswords() throws Exception {
    Path excel = tempDir.resolve("log-counts.xlsx");
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet();
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("Email");
      header.createCell(1).setCellValue("Name");
      Row valid = sheet.createRow(1);
      valid.createCell(0).setCellValue("a@example.com");
      valid.createCell(1).setCellValue("Ada");
      try (var out = java.nio.file.Files.newOutputStream(excel)) {
        workbook.write(out);
      }
    }
    Logger logger = (Logger) LoggerFactory.getLogger(ExcelReader.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      ExcelReader.read(excel.toString());
      String joined =
          String.join("\n", appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList());
      assertTrue(joined.contains("Excel file loaded: log-counts.xlsx"));
      assertTrue(joined.contains("Total rows: 1"));
      assertTrue(joined.contains("Valid contacts: 1"));
      assertTrue(joined.contains("Invalid contacts: 0"));
      assertTrue(appender.list.stream().noneMatch(e -> e.getFormattedMessage().toLowerCase().contains("password")));
    } finally {
      logger.detachAppender(appender);
    }
  }
}
