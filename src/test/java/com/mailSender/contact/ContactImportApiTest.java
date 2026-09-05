package com.mailSender.contact;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailSender.ApiTestSupport;
import com.mailSender.ApiTestSupport.RegisteredUser;
import java.io.ByteArrayOutputStream;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"api", "apitest"})
class ContactImportApiTest {

  @MockBean private JavaMailSender javaMailSender;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void csvImportReturnsSummaryAndRowErrorsWithoutExtraPii() throws Exception {
    RegisteredUser user = ApiTestSupport.register(mockMvc, objectMapper, "Importer");
    String listId = createList(user.token());
    String csv =
        """
        Email,Name,Company
        ada@example.com,Ada,Acme
        not-an-email,Bad,
        ,Missing,
        ada@example.com,Dup,Acme
        """;
    MockMultipartFile file =
        new MockMultipartFile("file", "people.csv", "text/csv", csv.getBytes());

    mockMvc
        .perform(
            multipart("/api/v1/contact-lists/" + listId + "/upload")
                .file(file)
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalRows").value(4))
        .andExpect(jsonPath("$.data.valid").value(1))
        .andExpect(jsonPath("$.data.invalid").value(2))
        .andExpect(jsonPath("$.data.duplicates").value(1))
        .andExpect(jsonPath("$.data.errors[0]").value("Row 3: Invalid email format"))
        .andExpect(jsonPath("$.data.errors[1]").value("Row 4: Missing email"))
        .andExpect(jsonPath("$.data.errors[2]").value("Row 5: Duplicate email"))
        .andExpect(jsonPath("$.data.errors[0]").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("ada@"))))
        .andExpect(jsonPath("$.data.list.totalContacts").value(1));
  }

  @Test
  void xlsxImportReusesExcelReader() throws Exception {
    RegisteredUser user = ApiTestSupport.register(mockMvc, objectMapper, "XlsxImp");
    String listId = createList(user.token());
    MockMultipartFile file =
        new MockMultipartFile("file", "people.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", xlsxBytes());

    mockMvc
        .perform(
            multipart("/api/v1/contact-lists/" + listId + "/upload")
                .file(file)
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.valid").value(2))
        .andExpect(jsonPath("$.data.list.totalContacts").value(2));
  }

  private String createList(String token) throws Exception {
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/contact-lists")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Upload list\"}"))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper.readTree(created.getResponse().getContentAsString()).at("/data/id").asText();
  }

  private static byte[] xlsxBytes() throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet();
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("Email");
      header.createCell(1).setCellValue("Name");
      Row one = sheet.createRow(1);
      one.createCell(0).setCellValue("ada@example.com");
      one.createCell(1).setCellValue("Ada");
      Row two = sheet.createRow(2);
      two.createCell(0).setCellValue("bob@example.com");
      two.createCell(1).setCellValue("Bob");
      workbook.write(out);
      return out.toByteArray();
    }
  }
}
