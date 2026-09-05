package com.mailSender.excel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class ExcelReaderTest {

  @TempDir Path tempDir;

  @Test
  @DisplayName("Valid Excel")
  void validExcelReturnsAllContacts() throws Exception {
    Path excel =
        writeXlsx(
            "valid.xlsx",
            sheet -> {
              writeRow(sheet, 0, "Email", "Name");
              writeRow(sheet, 1, "ada@example.com", "Ada");
              writeRow(sheet, 2, "bob@example.com", "Bob");
            });

    ExcelReadResult result = ExcelReader.read(excel.toString());
    assertEquals(2, result.getContacts().size());
    assertEquals(2, result.getTotalRows());
    assertEquals(2, result.getValid());
    assertEquals(0, result.getInvalid());
    assertEquals(0, result.getDuplicates());
    assertEquals("ada@example.com", result.getContacts().get(0).getEmail());
    assertEquals("Ada", result.getContacts().get(0).getName());
    assertEquals("bob@example.com", result.getContacts().get(1).getEmail());
    assertEquals("Bob", result.getContacts().get(1).getName());
  }

  @Test
  @DisplayName("Empty Excel")
  void emptyExcelFailsLoudly() throws Exception {
    Path excel = writeXlsx("empty.xlsx", sheet -> {});
    ExcelProcessingException ex =
        assertThrows(ExcelProcessingException.class, () -> ExcelReader.read(excel.toString()));
    assertTrue(ex.getMessage().contains("contains no rows"));
  }

  @Test
  @DisplayName("Missing email column")
  void missingEmailColumnFailsLoudly() throws Exception {
    Path excel =
        writeXlsx(
            "missing-email-column.xlsx",
            sheet -> {
              writeRow(sheet, 0, "Name", "Company");
              writeRow(sheet, 1, "Ada", "Acme");
            });

    ExcelProcessingException ex =
        assertThrows(ExcelProcessingException.class, () -> ExcelReader.read(excel.toString()));
    assertTrue(ex.getMessage().contains("the Email column was not found"));
  }

  @Test
  @DisplayName("Invalid emails")
  void invalidEmailsAreSkippedAndCounted() throws Exception {
    Path excel =
        writeXlsx(
            "invalid-emails.xlsx",
            sheet -> {
              writeRow(sheet, 0, "Email", "Name");
              writeRow(sheet, 1, "good@example.com", "Good");
              writeRow(sheet, 2, "not-an-email", "Bad");
              writeRow(sheet, 3, "also.invalid", "Worse");
            });

    ExcelReadResult result = ExcelReader.read(excel.toString());
    assertEquals(1, result.getContacts().size());
    assertEquals("good@example.com", result.getContacts().get(0).getEmail());
    assertEquals(3, result.getTotalRows());
    assertEquals(1, result.getValid());
    assertEquals(2, result.getInvalid());
    assertEquals(0, result.getDuplicates());
  }

  @Test
  @DisplayName("Duplicate emails")
  void duplicateEmailsKeepFirstAndCountLaterRows() throws Exception {
    Path excel =
        writeXlsx(
            "duplicates.xlsx",
            sheet -> {
              writeRow(sheet, 0, "Email", "Name");
              writeRow(sheet, 1, "a@example.com", "First");
              writeRow(sheet, 2, "A@Example.com", "Second");
              writeRow(sheet, 3, "b@example.com", "Other");
            });

    ExcelReadResult result = ExcelReader.read(excel.toString());
    assertEquals(2, result.getContacts().size());
    assertEquals("a@example.com", result.getContacts().get(0).getEmail());
    assertEquals("First", result.getContacts().get(0).getName());
    assertEquals("b@example.com", result.getContacts().get(1).getEmail());
    assertEquals(3, result.getTotalRows());
    assertEquals(2, result.getValid());
    assertEquals(0, result.getInvalid());
    assertEquals(1, result.getDuplicates());
  }

  @Test
  @DisplayName("Empty rows")
  void emptyRowsAreSkippedIncludingMissingAndBlankRows() throws Exception {
    Path excel =
        writeXlsx(
            "empty-rows.xlsx",
            sheet -> {
              writeRow(sheet, 0, "Email", "Name", "Company");
              writeRow(sheet, 1, "keep@example.com", "Keep", "Acme");
              // row index 2 is never created → ExcelReader treats it as an empty row
              writeRow(sheet, 3, "   ", "   ", "   ");
              writeRow(sheet, 4, "after@example.com", "After", "Beta");
            });

    ExcelReadResult result = ExcelReader.read(excel.toString());
    assertEquals(2, result.getContacts().size());
    assertEquals("keep@example.com", result.getContacts().get(0).getEmail());
    assertEquals("after@example.com", result.getContacts().get(1).getEmail());
    assertEquals(4, result.getTotalRows());
    assertEquals(2, result.getValid());
    assertEquals(2, result.getInvalid());
    assertEquals(0, result.getDuplicates());
  }

  @Test
  @DisplayName("Nine consecutive empty rows still allow later contacts")
  void nineConsecutiveEmptyRowsDoNotStopReading() throws Exception {
    Path excel =
        writeXlsx(
            "nine-empty-rows.xlsx",
            sheet -> {
              writeRow(sheet, 0, "Email", "Name");
              writeRow(sheet, 1, "keep@example.com", "Keep");
              for (int i = 2; i <= 10; i++) {
                writeRow(sheet, i, "   ", "   ");
              }
              writeRow(sheet, 11, "after@example.com", "After");
            });

    ExcelReadResult result = ExcelReader.read(excel.toString());
    assertEquals(2, result.getContacts().size());
    assertEquals("keep@example.com", result.getContacts().get(0).getEmail());
    assertEquals("after@example.com", result.getContacts().get(1).getEmail());
    assertEquals(11, result.getTotalRows());
    assertEquals(2, result.getValid());
    assertEquals(9, result.getInvalid());
  }

  @Test
  @DisplayName("Ten consecutive empty rows stop reading")
  void tenConsecutiveEmptyRowsStopReading() throws Exception {
    Path excel =
        writeXlsx(
            "ten-empty-rows.xlsx",
            sheet -> {
              writeRow(sheet, 0, "Email", "Name");
              writeRow(sheet, 1, "keep@example.com", "Keep");
              for (int i = 2; i <= 11; i++) {
                writeRow(sheet, i, "   ", "   ");
              }
              writeRow(sheet, 12, "ignored@example.com", "Ignored");
            });

    ExcelReadResult result = ExcelReader.read(excel.toString());
    assertEquals(1, result.getContacts().size());
    assertEquals("keep@example.com", result.getContacts().get(0).getEmail());
    assertEquals(11, result.getTotalRows());
    assertEquals(1, result.getValid());
    assertEquals(10, result.getInvalid());
    assertEquals(0, result.getDuplicates());
  }

  @Test
  @DisplayName("Multiple columns")
  void multipleColumnsBecomePlaceholders() throws Exception {
    Path excel =
        writeXlsx(
            "multi-column.xlsx",
            sheet -> {
              writeRow(sheet, 0, "Email", "Name", "Company", "Title");
              writeRow(sheet, 1, "ada@example.com", "Ada", "Acme", "Engineer");
            });

    ExcelReadResult result = ExcelReader.read(excel.toString());
    assertEquals(1, result.getContacts().size());
    Contact contact = result.getContacts().get(0);
    assertEquals("ada@example.com", contact.getEmail());
    assertEquals("Ada", contact.getName());
    assertEquals("Acme", contact.getPlaceholders().get("company"));
    assertEquals("Engineer", contact.getPlaceholders().get("title"));
    assertTrue(result.getPlaceholderKeys().contains("company"));
    assertTrue(result.getPlaceholderKeys().contains("title"));
  }

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
    Level previous = logger.getLevel();
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.setLevel(Level.INFO);
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
      logger.setLevel(previous);
    }
  }

  @FunctionalInterface
  private interface SheetWriter {
    void write(Sheet sheet) throws Exception;
  }

  private Path writeXlsx(String filename, SheetWriter writer) throws Exception {
    Path excel = tempDir.resolve(filename);
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet();
      writer.write(sheet);
      try (var out = Files.newOutputStream(excel)) {
        workbook.write(out);
      }
    }
    return excel;
  }

  private static void writeRow(Sheet sheet, int rowIndex, String... values) {
    Row row = sheet.createRow(rowIndex);
    for (int i = 0; i < values.length; i++) {
      row.createCell(i).setCellValue(values[i]);
    }
  }
}
