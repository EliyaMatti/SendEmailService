package com.mailSender.campaign;

import com.mailSender.common.exception.ApiException;
import com.mailSender.common.response.PageResponse;
import com.mailSender.common.security.AuthPrincipal;
import com.mailSender.config.ApplicationProfiles;
import com.mailSender.config.ExcelmailProperties;
import com.mailSender.config.SmtpConfiguration;
import com.mailSender.contact.ContactEntity;
import com.mailSender.contact.ContactEntityRepository;
import com.mailSender.contact.ContactList;
import com.mailSender.contact.ContactListRepository;
import com.mailSender.mailtemplate.StoredEmailTemplate;
import com.mailSender.mailtemplate.TemplateService;
import com.mailSender.organization.TenantService;
import com.mailSender.smtpaccount.SmtpAccount;
import com.mailSender.smtpaccount.SmtpAccountService;
import com.mailSender.usage.UsageService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile(ApplicationProfiles.API)
public class CampaignService {

  private static final Logger log = LoggerFactory.getLogger(CampaignService.class);

  private final CampaignRepository campaigns;
  private final CampaignRecipientRepository recipients;
  private final ContactListRepository lists;
  private final ContactEntityRepository contacts;
  private final TemplateService templates;
  private final SmtpAccountService smtpAccounts;
  private final TenantService tenants;
  private final ExcelmailProperties properties;
  private final UsageService usage;

  public CampaignService(
      CampaignRepository campaigns,
      CampaignRecipientRepository recipients,
      ContactListRepository lists,
      ContactEntityRepository contacts,
      TemplateService templates,
      SmtpAccountService smtpAccounts,
      TenantService tenants,
      ExcelmailProperties properties,
      UsageService usage) {
    this.campaigns = campaigns;
    this.recipients = recipients;
    this.lists = lists;
    this.contacts = contacts;
    this.templates = templates;
    this.smtpAccounts = smtpAccounts;
    this.tenants = tenants;
    this.properties = properties;
    this.usage = usage;
  }

  @Transactional
  public CampaignResponse create(AuthPrincipal principal, CreateCampaignRequest request) {
    UUID orgId = org(principal);
    ContactList list = requireList(orgId, request.getContactListId());
    StoredEmailTemplate template = templates.require(principal, request.getTemplateId());
    SmtpAccount smtp = smtpAccounts.require(principal, request.getSmtpAccountId());
    templates.validateAgainstList(template.getSubject(), template.getBody(), list);
    Campaign campaign = new Campaign();
    campaign.setOrganizationId(orgId);
    campaign.setName(request.getName().trim());
    campaign.setContactListId(list.getId());
    campaign.setTemplateId(template.getId());
    campaign.setSmtpAccountId(smtp.getId());
    campaign.setStatus(CampaignStatus.READY);
    campaigns.saveAndFlush(campaign);
    log.info("Campaign created");
    return toResponse(campaign);
  }

  @Transactional(readOnly = true)
  public PageResponse<CampaignResponse> list(AuthPrincipal principal, Pageable pageable) {
    return PageResponse.from(campaigns.findByOrganizationId(org(principal), pageable).map(this::toResponse));
  }

  @Transactional(readOnly = true)
  public CampaignResponse get(AuthPrincipal principal, UUID id) {
    return toResponse(require(principal, id));
  }

  @Transactional
  public CampaignResponse start(AuthPrincipal principal, UUID id) {
    Campaign campaign = require(principal, id);
    if (campaign.getStatus() != CampaignStatus.READY && campaign.getStatus() != CampaignStatus.DRAFT) {
      throw new ApiException("CAMPAIGN_INVALID_STATE", "Only draft or ready campaigns can be started.", 409);
    }
    validateForLaunch(principal, campaign);
    if (recipients.findByCampaignId(campaign.getId()).isEmpty()) {
      queueRecipients(campaign);
    }
    campaign.setStatus(CampaignStatus.RUNNING);
    campaign.setStartedAt(Instant.now());
    campaigns.saveAndFlush(campaign);
    log.info("Campaign started");
    return toResponse(campaign);
  }

  @Transactional
  public CampaignResponse pause(AuthPrincipal principal, UUID id) {
    Campaign campaign = require(principal, id);
    if (campaign.getStatus() != CampaignStatus.RUNNING) {
      throw new ApiException("CAMPAIGN_INVALID_STATE", "Only running campaigns can be paused.", 409);
    }
    campaign.setStatus(CampaignStatus.PAUSED);
    campaigns.saveAndFlush(campaign);
    log.info("Campaign paused");
    return toResponse(campaign);
  }

  @Transactional
  public CampaignResponse resume(AuthPrincipal principal, UUID id) {
    Campaign campaign = require(principal, id);
    if (campaign.getStatus() != CampaignStatus.PAUSED) {
      throw new ApiException("CAMPAIGN_INVALID_STATE", "Only paused campaigns can be resumed.", 409);
    }
    campaign.setStatus(CampaignStatus.RUNNING);
    campaigns.saveAndFlush(campaign);
    log.info("Campaign resumed");
    return toResponse(campaign);
  }

  @Transactional
  public CampaignResponse cancel(AuthPrincipal principal, UUID id) {
    Campaign campaign = require(principal, id);
    if (campaign.getStatus() == CampaignStatus.COMPLETED
        || campaign.getStatus() == CampaignStatus.CANCELLED) {
      throw new ApiException("CAMPAIGN_INVALID_STATE", "This campaign can no longer be cancelled.", 409);
    }
    campaign.setStatus(CampaignStatus.CANCELLED);
    campaign.setCompletedAt(Instant.now());
    for (CampaignRecipient recipient : recipients.findByCampaignId(campaign.getId())) {
      if (recipient.getStatus() == RecipientStatus.PENDING
          || recipient.getStatus() == RecipientStatus.PROCESSING) {
        recipient.setStatus(RecipientStatus.SKIPPED);
      }
    }
    campaigns.saveAndFlush(campaign);
    log.info("Campaign cancelled");
    return toResponse(campaign);
  }

  public Campaign require(AuthPrincipal principal, UUID id) {
    return campaigns
        .findByIdAndOrganizationId(id, org(principal))
        .orElseThrow(() -> new ApiException("CAMPAIGN_NOT_FOUND", "The campaign was not found.", 404));
  }

  void validateForLaunch(AuthPrincipal principal, Campaign campaign) {
    ContactList list = requireList(campaign.getOrganizationId(), campaign.getContactListId());
    StoredEmailTemplate template = templates.require(principal, campaign.getTemplateId());
    SmtpAccount smtp = smtpAccounts.require(principal, campaign.getSmtpAccountId());
    SmtpConfiguration configuration = smtpAccounts.toSmtpConfiguration(smtp);
    if (!configuration.isReadyForSend()) {
      throw new ApiException("SMTP_NOT_READY", "The SMTP account is missing required fields.", 400);
    }
    templates.validateAgainstList(template.getSubject(), template.getBody(), list);
    long contactCount =
        contacts.countByOrganizationIdAndContactListId(campaign.getOrganizationId(), list.getId());
    if (contactCount <= 0) {
      throw new ApiException("CAMPAIGN_EMPTY", "The contact list has no valid recipients.", 400);
    }
    if (contactCount > properties.getLimits().getMaxRecipientsPerCampaign()) {
      throw new ApiException(
          "CAMPAIGN_TOO_LARGE",
          "The campaign exceeds the maximum number of recipients.",
          400);
    }
    if (usage.emailsSentToday(campaign.getOrganizationId()) >= properties.getLimits().getMaxDailySends()) {
      throw new ApiException("DAILY_SEND_LIMIT", "The organization has reached the daily send limit.", 429);
    }
  }

  private void queueRecipients(Campaign campaign) {
    List<ContactEntity> listContacts =
        contacts.findByOrganizationIdAndContactListId(
            campaign.getOrganizationId(), campaign.getContactListId());
    List<CampaignRecipient> queued = new ArrayList<>();
    for (ContactEntity contact : listContacts) {
      CampaignRecipient recipient = new CampaignRecipient();
      recipient.setCampaign(campaign);
      recipient.setContactId(contact.getId());
      recipient.setEmail(contact.getEmail());
      recipient.setStatus(RecipientStatus.PENDING);
      queued.add(recipient);
    }
    recipients.saveAll(queued);
    campaign.setTotalRecipients(queued.size());
    campaign.setQueuedCount(queued.size());
  }

  private ContactList requireList(UUID orgId, UUID listId) {
    return lists
        .findByIdAndOrganizationId(listId, orgId)
        .orElseThrow(
            () -> new ApiException("CONTACT_LIST_NOT_FOUND", "The contact list was not found.", 404));
  }

  private CampaignResponse toResponse(Campaign campaign) {
    return new CampaignResponse(
        campaign.getId(),
        campaign.getOrganizationId(),
        campaign.getName(),
        campaign.getContactListId(),
        campaign.getTemplateId(),
        campaign.getSmtpAccountId(),
        campaign.getStatus(),
        campaign.getTotalRecipients(),
        campaign.getQueuedCount(),
        campaign.getSentCount(),
        campaign.getFailedCount(),
        campaign.getCreatedAt(),
        campaign.getStartedAt(),
        campaign.getCompletedAt(),
        campaign.getUpdatedAt());
  }

  private UUID org(AuthPrincipal principal) {
    tenants.requireMembership(principal.userId(), principal.organizationId());
    return principal.organizationId();
  }
}
