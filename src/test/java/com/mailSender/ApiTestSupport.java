package com.mailSender;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class ApiTestSupport {

  private ApiTestSupport() {}

  public static RegisteredUser register(MockMvc mockMvc, ObjectMapper mapper, String name)
      throws Exception {
    String email = name.toLowerCase() + "-" + System.nanoTime() + "@example.com";
    String body =
        """
        {"name":"%s","email":"%s","password":"secret-pass"}
        """
            .formatted(name, email);
    MvcResult result =
        mockMvc
            .perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode data = mapper.readTree(result.getResponse().getContentAsString()).get("data");
    return new RegisteredUser(
        data.get("token").asText(),
        data.get("user").get("id").asText(),
        data.get("user").get("organizationId").asText(),
        email);
  }

  public record RegisteredUser(String token, String userId, String organizationId, String email) {}
}
