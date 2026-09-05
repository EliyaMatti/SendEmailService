package com.mailSender.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mailSender.campaign.Campaign;
import com.mailSender.campaign.CampaignRecipient;
import com.mailSender.campaign.CampaignRecipientRepository;
import com.mailSender.campaign.CampaignRepository;
import com.mailSender.campaign.CampaignStatus;
import com.mailSender.campaign.RecipientStatus;
import com.mailSender.contact.ContactEntity;
import com.mailSender.contact.ContactEntityRepository;
import com.mailSender.contact.ContactList;
import com.mailSender.contact.ContactListRepository;
import com.mailSender.contact.ContactStatus;
import com.mailSender.mailtemplate.StoredEmailTemplate;
import com.mailSender.mailtemplate.StoredEmailTemplateRepository;
import com.mailSender.organization.MemberRole;
import com.mailSender.organization.Organization;
import com.mailSender.organization.OrganizationMember;
import com.mailSender.organization.OrganizationMemberRepository;
import com.mailSender.organization.OrganizationRepository;
import com.mailSender.smtp.EmailSender;
import com.mailSender.smtp.SmtpSendException;
import com.mailSender.smtpaccount.AesGcmEncryptor;
import com.mailSender.smtpaccount.SmtpAccount;
import com.mailSender.smtpaccount.SmtpAccountRepository;
import com.mailSender.user.UserAccount;
import com.mailSender.user.UserAccountRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"api", "apitest"})
class CampaignWorkerRetryTest {

  @MockBean private JavaMailSender javaMailSender;
  @MockBean private EmailSender emailSender;

  @Autowired private CampaignWorker worker;
  @Autowired private UserAccountRepository users;
  @Autowired private OrganizationRepository organizations;
  @Autowired private OrganizationMemberRepository members;
  @Autowired private ContactListRepository lists;
  @Autowired private ContactEntityRepository contacts;
  @Autowired private StoredEmailTemplateRepository templates;
  @Autowired private SmtpAccountRepository smtpAccounts;
  @Autowired private CampaignRepository campaigns;
  @Autowired private CampaignRecipientRepository recipients;
  @Autowired private AesGcmEncryptor encryptor;

  @Test
  @Transactional
  void retriesThenFailsPermanentInvalidRecipient() {
    doThrow(new SmtpSendException("SMTP rejected recipient ada@example.com as invalid.", new RuntimeException("invalid addresses")))
        .when(emailSender)
        .send(any());

    UserAccount user = new UserAccount();
    user.setEmail("worker-" + System.nanoTime() + "@example.com");
    user.setName("Worker");
    user.setPasswordHash("hash");
    users.saveAndFlush(user);
    Organization org = new Organization();
    org.setName("Org");
    org.setOwnerId(user.getId());
    organizations.saveAndFlush(org);
    OrganizationMember member = new OrganizationMember();
    member.setOrganizationId(org.getId());
    member.setUserId(user.getId());
    member.setRole(MemberRole.OWNER);
    members.saveAndFlush(member);

    ContactList list = new ContactList();
    list.setOrganizationId(org.getId());
    list.setName("List");
    list.setTotalContacts(1);
    lists.saveAndFlush(list);
    ContactEntity contact = new ContactEntity();
    contact.setOrganizationId(org.getId());
    contact.setContactListId(list.getId());
    contact.setEmail("ada@example.com");
    contact.setName("Ada");
    contact.setStatus(ContactStatus.ACTIVE);
    contacts.saveAndFlush(contact);

    StoredEmailTemplate template = new StoredEmailTemplate();
    template.setOrganizationId(org.getId());
    template.setName("T");
    template.setSubject("Hi");
    template.setBody("Hello {{name}}");
    templates.saveAndFlush(template);

    SmtpAccount smtp = new SmtpAccount();
    smtp.setOrganizationId(org.getId());
    smtp.setProvider("gmail");
    smtp.setHost("smtp.gmail.com");
    smtp.setPort(587);
    smtp.setUsername("user@example.com");
    smtp.setEncryptedPassword(encryptor.encrypt("secret"));
    smtp.setKeyVersion(encryptor.currentKeyVersion());
    smtp.setFromEmail("user@example.com");
    smtp.setTlsEnabled(true);
    smtpAccounts.saveAndFlush(smtp);

    Campaign campaign = new Campaign();
    campaign.setOrganizationId(org.getId());
    campaign.setName("C");
    campaign.setContactListId(list.getId());
    campaign.setTemplateId(template.getId());
    campaign.setSmtpAccountId(smtp.getId());
    campaign.setStatus(CampaignStatus.RUNNING);
    campaigns.saveAndFlush(campaign);

    CampaignRecipient recipient = new CampaignRecipient();
    recipient.setCampaign(campaign);
    recipient.setContactId(contact.getId());
    recipient.setEmail(contact.getEmail());
    recipient.setStatus(RecipientStatus.PENDING);
    recipients.saveAndFlush(recipient);

    worker.processNext();
    CampaignRecipient after = recipients.findById(recipient.getId()).orElseThrow();
    Assertions.assertEquals(RecipientStatus.FAILED, after.getStatus());
    verify(emailSender, times(1)).send(any());
  }
}
