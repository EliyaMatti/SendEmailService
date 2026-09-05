package com.mailSender.contact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mailSender.organization.MemberRole;
import com.mailSender.organization.Organization;
import com.mailSender.organization.OrganizationMember;
import com.mailSender.organization.OrganizationMemberRepository;
import com.mailSender.organization.OrganizationRepository;
import com.mailSender.user.UserAccount;
import com.mailSender.user.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"api", "apitest"})
@Transactional
class ContactListRepositoryTest {

  @MockBean private JavaMailSender javaMailSender;

  @Autowired private UserAccountRepository users;
  @Autowired private OrganizationRepository organizations;
  @Autowired private OrganizationMemberRepository members;
  @Autowired private ContactListRepository lists;

  @Test
  void listsAreScopedByOrganization() {
    UserAccount alice = user("repo-alice@example.com", "Alice");
    UserAccount bob = user("repo-bob@example.com", "Bob");
    Organization acme = org("Acme", alice.getId());
    Organization other = org("Other", bob.getId());
    member(acme.getId(), alice.getId());
    member(other.getId(), bob.getId());
    ContactList a = new ContactList();
    a.setOrganizationId(acme.getId());
    a.setName("A");
    lists.saveAndFlush(a);
    ContactList b = new ContactList();
    b.setOrganizationId(other.getId());
    b.setName("B");
    lists.saveAndFlush(b);

    assertEquals(1, lists.findByOrganizationId(acme.getId(), PageRequest.of(0, 10)).getTotalElements());
    assertTrue(lists.findByIdAndOrganizationId(b.getId(), acme.getId()).isEmpty());
  }

  private UserAccount user(String email, String name) {
    UserAccount user = new UserAccount();
    user.setEmail(email);
    user.setPasswordHash("hash");
    user.setName(name);
    return users.saveAndFlush(user);
  }

  private Organization org(String name, java.util.UUID ownerId) {
    Organization organization = new Organization();
    organization.setName(name);
    organization.setOwnerId(ownerId);
    return organizations.saveAndFlush(organization);
  }

  private void member(java.util.UUID orgId, java.util.UUID userId) {
    OrganizationMember member = new OrganizationMember();
    member.setOrganizationId(orgId);
    member.setUserId(userId);
    member.setRole(MemberRole.OWNER);
    members.saveAndFlush(member);
  }
}
