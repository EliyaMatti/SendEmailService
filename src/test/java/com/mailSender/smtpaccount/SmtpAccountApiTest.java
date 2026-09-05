package com.mailSender.smtpaccount;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailSender.ApiTestSupport;
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
class SmtpAccountApiTest {

  @MockBean private JavaMailSender javaMailSender;
  @MockBean private SmtpConnectionTester smtpConnectionTester;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void createHidesPasswordAndTestUsesMock() throws Exception {
    RegisteredUser user = ApiTestSupport.register(mockMvc, objectMapper, "SmtpUser");
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/smtp")
                    .header("Authorization", "Bearer " + user.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"provider":"gmail","host":"smtp.gmail.com","port":587,"username":"user@example.com","password":"app-password-value","fromEmail":"user@example.com","fromName":"User","tlsEnabled":true}
                        """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.host").value("smtp.gmail.com"))
            .andExpect(jsonPath("$.data.password").doesNotExist())
            .andExpect(jsonPath("$.data.encryptedPassword").doesNotExist())
            .andReturn();
    String id = objectMapper.readTree(created.getResponse().getContentAsString()).at("/data/id").asText();

    doNothing().when(smtpConnectionTester).test(any());
    mockMvc
        .perform(
            post("/api/v1/smtp/" + id + "/test")
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.success").value(true));

    doThrow(new RuntimeException("nope")).when(smtpConnectionTester).test(any());
    mockMvc
        .perform(
            post("/api/v1/smtp/" + id + "/test")
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.success").value(false))
        .andExpect(jsonPath("$.data.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("app-password"))));
  }
}
