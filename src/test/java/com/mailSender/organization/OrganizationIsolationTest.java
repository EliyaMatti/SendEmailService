package com.mailSender.organization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mailSender.common.exception.ApiException;
import com.mailSender.user.UserAccount;
import com.mailSender.user.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"api", "apitest"})
@Transactional
class OrganizationIsolationTest {

  @MockBean private JavaMailSender javaMailSender;

  @Autowired private UserAccountRepository users;
  @Autowired private OrganizationRepository organizations;
  @Autowired private OrganizationMemberRepository members;
  @Autowired private TenantService tenants;

  @Test
  void userCannotAccessAnotherOrganization() {
    UserAccount alice = user("alice@example.com", "Alice");
    UserAccount bob = user("bob@example.com", "Bob");
    Organization acme = org("Acme", alice.getId());
    Organization other = org("Other", bob.getId());
    member(acme.getId(), alice.getId(), MemberRole.OWNER);
    member(other.getId(), bob.getId(), MemberRole.OWNER);

    OrganizationMember aliceAcme = tenants.requireMembership(alice.getId(), acme.getId());
    assertEquals(MemberRole.OWNER, aliceAcme.getRole());

    ApiException denied =
        assertThrows(
            ApiException.class, () -> tenants.requireMembership(alice.getId(), other.getId()));
    assertEquals("ORGANIZATION_ACCESS_DENIED", denied.getCode());
    assertEquals(403, denied.getHttpStatus());
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

  private void member(java.util.UUID orgId, java.util.UUID userId, MemberRole role) {
    OrganizationMember member = new OrganizationMember();
    member.setOrganizationId(orgId);
    member.setUserId(userId);
    member.setRole(role);
    members.saveAndFlush(member);
  }
}
