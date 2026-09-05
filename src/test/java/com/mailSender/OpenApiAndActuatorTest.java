package com.mailSender;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"api", "apitest"})
class OpenApiAndActuatorTest {

  @MockBean private JavaMailSender javaMailSender;

  @Autowired private MockMvc mockMvc;

  @Test
  void openApiAndSafeActuatorArePublic() throws Exception {
    mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    mockMvc
        .perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
    mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
    mockMvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
  }
}
