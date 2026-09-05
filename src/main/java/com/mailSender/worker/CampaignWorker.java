package com.mailSender.worker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailSender.campaign.Campaign;
import com.mailSender.campaign.CampaignRecipient;
import com.mailSender.campaign.CampaignRecipientRepository;
import com.mailSender.campaign.CampaignRepository;
import com.mailSender.campaign.CampaignStatus;
import com.mailSender.campaign.EmailComposer;
import com.mailSender.campaign.EmailMessage;
import com.mailSender.campaign.RecipientStatus;
import com.mailSender.config.ApplicationProfiles;
import com.mailSender.config.ExcelmailProperties;
import com.mailSender.config.MailAppProperties;
import com.mailSender.config.SmtpConfiguration;
import com.mailSender.contact.ContactEntity;
import com.mailSender.contact.ContactEntityRepository;
import com.mailSender.excel.Contact;
import com.mailSender.mailtemplate.StoredEmailTemplate;
import com.mailSender.mailtemplate.StoredEmailTemplateRepository;
import com.mailSender.smtp.EmailSender;
import com.mailSender.smtp.SmtpFailureClassifier;
import com.mailSender.smtpaccount.SmtpAccount;
import com.mailSender.smtpaccount.SmtpAccountRepository;
import com.mailSender.smtpaccount.SmtpAccountService;
import com.mailSender.usage.UsageService;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile(ApplicationProfiles.API)
public class CampaignWorker {

  private static final Logger log = LoggerFactory.getLogger(CampaignWorker.class);
  private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {};

  private final ExcelmailProperties excelmailProperties;
  private final MailAppProperties mailAppProperties;
  private final CampaignRecipientRepository recipients;
  private final CampaignRepository campaigns;
  private final ContactEntityRepository contacts;
  private final StoredEmailTemplateRepository templates;
  private final SmtpAccountRepository smtpAccounts;
  private final SmtpAccountService smtpAccountService;
  private final EmailComposer emailComposer;
  private final EmailSender emailSender;
  private final UsageService usage;
  private final ObjectMapper objectMapper;

  public CampaignWorker(
      ExcelmailProperties excelmailProperties,
      MailAppProperties mailAppProperties,
      CampaignRecipientRepository recipients,
      CampaignRepository campaigns,
      ContactEntityRepository contacts,
      StoredEmailTemplateRepository templates,
      SmtpAccountRepository smtpAccounts,
      SmtpAccountService smtpAccountService,
      EmailComposer emailComposer,
      EmailSender emailSender,
      UsageService usage,
      ObjectMapper objectMapper) {
    this.excelmailProperties = excelmailProperties;
    this.mailAppProperties = mailAppProperties;
    this.recipients = recipients;
    this.campaigns = campaigns;
    this.contacts = contacts;
    this.templates = templates;
    this.smtpAccounts = smtpAccounts;
    this.smtpAccountService = smtpAccountService;
    this.emailComposer = emailComposer;
    this.emailSender = emailSender;
    this.usage = usage;
    this.objectMapper = objectMapper;
  }

  @Scheduled(fixedDelayString = "${excelmail.worker.poll-ms:2000}")
  public void scheduledPoll() {
    if (!excelmailProperties.getWorker().isEnabled()) {
      return;
    }
    processNext();
  }

  @Transactional
  public void processNext() {
    var claimable = recipients.findClaimable(PageRequest.of(0, 1));
    if (claimable.isEmpty()) {
      return;
    }
    CampaignRecipient pending = claimable.get(0);
    Campaign campaign = pending.getCampaign();
    if (campaign.getStatus() != CampaignStatus.RUNNING) {
      return;
    }
    long sentLastMinute =
        recipients.countByCampaign_OrganizationIdAndStatusAndSentAtAfter(
            campaign.getOrganizationId(), RecipientStatus.SENT, Instant.now().minusSeconds(60));
    if (sentLastMinute >= excelmailProperties.getLimits().getMaxSendsPerMinute()) {
      log.info("Provider send-per-minute limit reached; skipping this poll");
      return;
    }
    int claimed = recipients.claimPending(pending.getId());
    if (claimed != 1) {
      return;
    }
    CampaignRecipient recipient = recipients.findById(pending.getId()).orElseThrow();
    recipient.setAttemptCount(recipient.getAttemptCount() + 1);
    try {
      ContactEntity stored =
          contacts
              .findById(recipient.getContactId())
              .orElseThrow(() -> new IllegalStateException("Contact missing"));
      StoredEmailTemplate template =
          templates.findById(campaign.getTemplateId()).orElseThrow();
      SmtpAccount smtp = smtpAccounts.findById(campaign.getSmtpAccountId()).orElseThrow();
      SmtpConfiguration configuration = smtpAccountService.toSmtpConfiguration(smtp);
      Contact contact = toContact(stored);
      EmailMessage message =
          emailComposer.composeCampaign(
              contact, template.getSubject(), template.getBody(), configuration.getFromEmail());
      emailSender.send(message);
      if (!mailAppProperties.isDryRun() && mailAppProperties.getSendDelayMs() > 0) {
        Thread.sleep(mailAppProperties.getSendDelayMs());
      }
      recipient.setStatus(RecipientStatus.SENT);
      recipient.setSentAt(Instant.now());
      recipient.setLastError(null);
      campaign.setSentCount(campaign.getSentCount() + 1);
      usage.increment(campaign.getOrganizationId(), true);
      log.info("Campaign recipient sent");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      fail(recipient, campaign, e);
    } catch (Exception e) {
      fail(recipient, campaign, e);
    }
    recipients.saveAndFlush(recipient);
    refreshCampaign(campaign);
  }

  private void fail(CampaignRecipient recipient, Campaign campaign, Exception error) {
    String message = SmtpFailureClassifier.userMessage(recipient.getEmail(), error);
    boolean permanent = SmtpFailureClassifier.isPermanent(error);
    int maxAttempts = excelmailProperties.getWorker().getMaxAttempts();
    if (!permanent && recipient.getAttemptCount() < maxAttempts) {
      recipient.setStatus(RecipientStatus.PENDING);
      recipient.setLastError(truncate(message));
      log.info("Campaign recipient scheduled for retry");
      return;
    }
    recipient.setStatus(RecipientStatus.FAILED);
    recipient.setLastError(truncate(message));
    campaign.setFailedCount(campaign.getFailedCount() + 1);
    usage.increment(campaign.getOrganizationId(), false);
    log.info("Campaign recipient failed");
  }

  private void refreshCampaign(Campaign campaign) {
    long pending = recipients.countByCampaignIdAndStatus(campaign.getId(), RecipientStatus.PENDING);
    long processing =
        recipients.countByCampaignIdAndStatus(campaign.getId(), RecipientStatus.PROCESSING);
    campaign.setQueuedCount((int) pending);
    if (campaign.getStatus() == CampaignStatus.RUNNING && pending == 0 && processing == 0) {
      campaign.setStatus(CampaignStatus.COMPLETED);
      campaign.setCompletedAt(Instant.now());
      log.info("Campaign completed");
    }
    campaigns.saveAndFlush(campaign);
  }

  private Contact toContact(ContactEntity stored) {
    Map<String, String> extras = Map.of();
    if (stored.getMetadataJson() != null && !stored.getMetadataJson().isBlank()) {
      try {
        extras = objectMapper.readValue(stored.getMetadataJson(), STRING_MAP);
      } catch (Exception ignored) {
        extras = Map.of();
      }
    }
    return new Contact(stored.getEmail(), stored.getName(), extras);
  }

  private static String truncate(String message) {
    if (message == null) {
      return null;
    }
    return message.length() <= 512 ? message : message.substring(0, 512);
  }
}
