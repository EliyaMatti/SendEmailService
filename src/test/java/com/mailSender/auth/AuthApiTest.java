package com.mailSender.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class AuthApiTest {

  @MockBean private JavaMailSender javaMailSender;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void registerLoginAndMeHidePasswordHash() throws Exception {
    String email = "peter-" + System.nanoTime() + "@example.com";
    String body =
        """
        {"name":"Peter","email":"%s","password":"secret-pass"}
        """
            .formatted(email);

    MvcResult register =
        mockMvc
            .perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").isNotEmpty())
            .andExpect(jsonPath("$.data.user.email").value(email))
            .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
            .andReturn();

    String token =
        objectMapper.readTree(register.getResponse().getContentAsString()).at("/data/token").asText();

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"%s","password":"secret-pass"}
                    """
                        .formatted(email)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.token").isNotEmpty());

    mockMvc
        .perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.email").value(email))
        .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

    mockMvc
        .perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("DUPLICATE_EMAIL"));
  }

  @Test
  void meRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isForbidden());
  }
}
