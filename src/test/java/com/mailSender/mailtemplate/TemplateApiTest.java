package com.mailSender.mailtemplate;

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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"api", "apitest"})
class TemplateApiTest {

  @MockBean private JavaMailSender javaMailSender;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void createTemplateAndRejectUnknownPlaceholder() throws Exception {
    RegisteredUser user = ApiTestSupport.register(mockMvc, objectMapper, "TplUser");

    mockMvc
        .perform(
            post("/api/v1/templates")
                .header("Authorization", "Bearer " + user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Intro","subject":"Hi {{Name}}","body":"Hello {{Name}} at {{email}}"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("Intro"));

    mockMvc
        .perform(
            post("/api/v1/templates")
                .header("Authorization", "Bearer " + user.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Bad","subject":"Hi","body":"Hello {{Company}}"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("TEMPLATE_INVALID"));
  }
}
