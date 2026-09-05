package com.mailSender.campaign;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailSender.ApiTestSupport;
import com.mailSender.ApiTestSupport.RegisteredUser;
import com.mailSender.smtp.EmailSender;
import com.mailSender.worker.CampaignWorker;
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
class CampaignLifecycleApiTest {

  @MockBean private JavaMailSender javaMailSender;
  @MockBean private EmailSender emailSender;
  @MockBean private com.mailSender.smtpaccount.SmtpConnectionTester smtpConnectionTester;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private CampaignWorker campaignWorker;

  @Test
  void createStartPauseResumeCancelAndWorkerSend() throws Exception {
    doNothing().when(emailSender).send(any());
    RegisteredUser user = ApiTestSupport.register(mockMvc, objectMapper, "CampUser");
    String listId = createList(user.token());
    uploadCsv(user.token(), listId);
    String templateId = createTemplate(user.token());
    String smtpId = createSmtp(user.token());

    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/campaigns")
                    .header("Authorization", "Bearer " + user.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Launch","contactListId":"%s","templateId":"%s","smtpAccountId":"%s"}
                        """
                            .formatted(listId, templateId, smtpId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("READY"))
            .andReturn();
    String campaignId =
        objectMapper.readTree(created.getResponse().getContentAsString()).at("/data/id").asText();

    mockMvc
        .perform(
            post("/api/v1/campaigns/" + campaignId + "/start")
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("RUNNING"));

    campaignWorker.processNext();
    campaignWorker.processNext();

    mockMvc
        .perform(get("/api/v1/campaigns/" + campaignId).header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.sentCount").value(2));

    verify(emailSender, times(2)).send(any());

    RegisteredUser other = ApiTestSupport.register(mockMvc, objectMapper, "OtherCamp");
    mockMvc
        .perform(get("/api/v1/campaigns/" + campaignId).header("Authorization", "Bearer " + other.token()))
        .andExpect(status().isNotFound());
  }

  @Test
  void pauseResumeAndCancel() throws Exception {
    doNothing().when(emailSender).send(any());
    RegisteredUser user = ApiTestSupport.register(mockMvc, objectMapper, "PauseUser");
    String listId = createList(user.token());
    uploadCsv(user.token(), listId);
    String templateId = createTemplate(user.token());
    String smtpId = createSmtp(user.token());
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/campaigns")
                    .header("Authorization", "Bearer " + user.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Control","contactListId":"%s","templateId":"%s","smtpAccountId":"%s"}
                        """
                            .formatted(listId, templateId, smtpId)))
            .andExpect(status().isOk())
            .andReturn();
    String campaignId =
        objectMapper.readTree(created.getResponse().getContentAsString()).at("/data/id").asText();

    mockMvc
        .perform(
            post("/api/v1/campaigns/" + campaignId + "/start")
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(jsonPath("$.data.status").value("RUNNING"));
    mockMvc
        .perform(
            post("/api/v1/campaigns/" + campaignId + "/pause")
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(jsonPath("$.data.status").value("PAUSED"));
    mockMvc
        .perform(
            post("/api/v1/campaigns/" + campaignId + "/resume")
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(jsonPath("$.data.status").value("RUNNING"));
    mockMvc
        .perform(
            post("/api/v1/campaigns/" + campaignId + "/cancel")
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(jsonPath("$.data.status").value("CANCELLED"));
  }

  private String createList(String token) throws Exception {
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/contact-lists")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Recipients\"}"))
            .andReturn();
    return objectMapper.readTree(created.getResponse().getContentAsString()).at("/data/id").asText();
  }

  private void uploadCsv(String token, String listId) throws Exception {
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "people.csv",
            "text/csv",
            "Email,Name\nada@example.com,Ada\nbob@example.com,Bob\n".getBytes());
    mockMvc
        .perform(
            multipart("/api/v1/contact-lists/" + listId + "/upload")
                .file(file)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  private String createTemplate(String token) throws Exception {
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/templates")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"name\":\"Hi\",\"subject\":\"Hello {{name}}\",\"body\":\"Hi {{name}} {{email}}\"}"))
            .andReturn();
    return objectMapper.readTree(created.getResponse().getContentAsString()).at("/data/id").asText();
  }

  private String createSmtp(String token) throws Exception {
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/smtp")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"provider\":\"gmail\",\"host\":\"smtp.gmail.com\",\"port\":587,\"username\":\"user@example.com\",\"password\":\"app-pass-word\",\"fromEmail\":\"user@example.com\",\"tlsEnabled\":true}"))
            .andReturn();
    return objectMapper.readTree(created.getResponse().getContentAsString()).at("/data/id").asText();
  }
}
