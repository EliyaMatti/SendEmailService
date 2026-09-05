package com.mailSender.contact;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class ContactListApiTest {

  @MockBean private JavaMailSender javaMailSender;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void createListGetAndDelete() throws Exception {
    RegisteredUser user = ApiTestSupport.register(mockMvc, objectMapper, "ContactOwner");

    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/contact-lists")
                    .header("Authorization", "Bearer " + user.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Prospects\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("Prospects"))
            .andExpect(jsonPath("$.data.totalContacts").value(0))
            .andReturn();

    String id =
        objectMapper
            .readTree(created.getResponse().getContentAsString())
            .at("/data/id")
            .asText();

    mockMvc
        .perform(get("/api/v1/contact-lists").header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].name").value("Prospects"))
        .andExpect(jsonPath("$.data.totalItems").value(1));

    mockMvc
        .perform(get("/api/v1/contact-lists/" + id).header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(id));

    mockMvc
        .perform(
            get("/api/v1/contact-lists/" + id + "/contacts")
                .header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalItems").value(0));

    mockMvc
        .perform(delete("/api/v1/contact-lists/" + id).header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/api/v1/contact-lists/" + id).header("Authorization", "Bearer " + user.token()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("CONTACT_LIST_NOT_FOUND"));
  }

  @Test
  void otherOrgCannotReadList() throws Exception {
    RegisteredUser alice = ApiTestSupport.register(mockMvc, objectMapper, "AliceLists");
    RegisteredUser bob = ApiTestSupport.register(mockMvc, objectMapper, "BobLists");

    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/contact-lists")
                    .header("Authorization", "Bearer " + alice.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Alice only\"}"))
            .andExpect(status().isOk())
            .andReturn();
    String id =
        objectMapper.readTree(created.getResponse().getContentAsString()).at("/data/id").asText();

    mockMvc
        .perform(get("/api/v1/contact-lists/" + id).header("Authorization", "Bearer " + bob.token()))
        .andExpect(status().isNotFound());
  }
}
