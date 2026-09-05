package com.mailSender;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * M1-034: layered packages must not import up the pipeline (Excel → Contact → template →
 * EmailMessage → EmailSender → SMTP).
 */
class ArchitectureLayeringTest {

  private static final Path MAIN_JAVA = Path.of("src/main/java/com/mailSender");

  @Test
  void excelDoesNotDependOnTemplateCampaignSmtpOrConfig() throws Exception {
    assertNoForbiddenImports("excel", List.of("template", "campaign", "smtp", "config"));
  }

  @Test
  void templateDoesNotDependOnCampaignSmtpOrConfig() throws Exception {
    assertNoForbiddenImports("template", List.of("campaign", "smtp", "config"));
  }

  @Test
  void campaignDoesNotDependOnSmtp() throws Exception {
    assertNoForbiddenImports("campaign", List.of("smtp"));
  }

  @Test
  void smtpDoesNotDependOnExcelOrTemplate() throws Exception {
    assertNoForbiddenImports("smtp", List.of("excel", "template"));
  }

  @Test
  void configDoesNotDependOnExcelTemplateCampaignOrSmtp() throws Exception {
    assertNoForbiddenImports("config", List.of("excel", "template", "campaign", "smtp"));
  }

  private static void assertNoForbiddenImports(String packageName, List<String> forbidden)
      throws IOException {
    Path dir = MAIN_JAVA.resolve(packageName);
    assertTrue(Files.isDirectory(dir), "missing package " + packageName);
    try (Stream<Path> files = Files.walk(dir)) {
      files
          .filter(path -> path.toString().endsWith(".java"))
          .forEach(path -> checkFile(path, packageName, forbidden));
    }
  }

  private static void checkFile(Path path, String packageName, List<String> forbidden) {
    String source;
    try {
      source = Files.readString(path, StandardCharsets.UTF_8);
    } catch (IOException e) {
      fail("could not read " + path + ": " + e.getMessage());
      return;
    }
    for (String other : forbidden) {
      String prefix = "import com.mailSender." + other + ".";
      if (source.contains(prefix)) {
        fail(
            packageName
                + " must not import "
                + other
                + " ("
                + path.getFileName()
                + " contains "
                + prefix
                + ")");
      }
    }
  }
}
