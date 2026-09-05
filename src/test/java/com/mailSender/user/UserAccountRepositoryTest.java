package com.mailSender.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"api", "apitest"})
@Transactional
class UserAccountRepositoryTest {

  @MockBean private JavaMailSender javaMailSender;

  @Autowired private UserAccountRepository users;

  @Test
  void persistsUniqueEmail() {
    UserAccount user = new UserAccount();
    user.setEmail("owner@example.com");
    user.setPasswordHash("not-a-real-hash");
    user.setName("Owner");
    users.saveAndFlush(user);
    assertTrue(users.existsByEmailIgnoreCase("OWNER@example.com"));
    assertEquals(
        "owner@example.com",
        users.findByEmailIgnoreCase("owner@example.com").orElseThrow().getEmail());
  }

  @Test
  void duplicateEmailIsRejected() {
    UserAccount first = new UserAccount();
    first.setEmail("dup@example.com");
    first.setPasswordHash("h1");
    first.setName("One");
    users.saveAndFlush(first);
    UserAccount second = new UserAccount();
    second.setEmail("dup@example.com");
    second.setPasswordHash("h2");
    second.setName("Two");
    assertThrows(DataIntegrityViolationException.class, () -> users.saveAndFlush(second));
  }
}
