package com.mailSender;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailSender.ApiTestSupport.RegisteredUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"api", "apitest"})
class TenantIsolationApiTest {

  @MockBean private JavaMailSender javaMailSender;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void organizationBCannotReadOrganizationAResources() throws Exception {
    RegisteredUser alice = ApiTestSupport.register(mockMvc, objectMapper, "IsoAlice");
    RegisteredUser bob = ApiTestSupport.register(mockMvc, objectMapper, "IsoBob");

    String listId = create(alice, "/api/v1/contact-lists", "{\"name\":\"A list\"}");
    String templateId =
        create(
            alice,
            "/api/v1/templates",
            "{\"name\":\"A tpl\",\"subject\":\"Hi {{name}}\",\"body\":\"Hello {{email}}\"}");
    String smtpId =
        create(
            alice,
            "/api/v1/smtp",
            "{\"provider\":\"gmail\",\"host\":\"smtp.gmail.com\",\"port\":587,\"username\":\"a@example.com\",\"password\":\"secret-pass\",\"fromEmail\":\"a@example.com\",\"tlsEnabled\":true}");
    String campaignId =
        create(
            alice,
            "/api/v1/campaigns",
            "{\"name\":\"A camp\",\"contactListId\":\"%s\",\"templateId\":\"%s\",\"smtpAccountId\":\"%s\"}"
                .formatted(listId, templateId, smtpId));

    mockMvc
        .perform(get("/api/v1/contact-lists/" + listId).header("Authorization", "Bearer " + bob.token()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/templates/" + templateId).header("Authorization", "Bearer " + bob.token()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/smtp/" + smtpId).header("Authorization", "Bearer " + bob.token()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.data.password").doesNotExist());
    mockMvc
        .perform(get("/api/v1/campaigns/" + campaignId).header("Authorization", "Bearer " + bob.token()))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/usage").header("Authorization", "Bearer " + bob.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].organizationId").value(bob.organizationId()));
  }

  private String create(RegisteredUser user, String path, String body) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post(path)
                    .header("Authorization", "Bearer " + user.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isOk())
            .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asText();
  }
}
