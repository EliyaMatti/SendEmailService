package com.mailSender.usage;

import com.mailSender.campaign.CampaignRepository;
import com.mailSender.common.response.PageResponse;
import com.mailSender.common.security.AuthPrincipal;
import com.mailSender.config.ApplicationProfiles;
import com.mailSender.contact.ContactEntityRepository;
import com.mailSender.organization.TenantService;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile(ApplicationProfiles.API)
public class UsageService {

  private final UsageRecordRepository records;
  private final CampaignRepository campaigns;
  private final ContactEntityRepository contacts;
  private final TenantService tenants;

  public UsageService(
      UsageRecordRepository records,
      CampaignRepository campaigns,
      ContactEntityRepository contacts,
      TenantService tenants) {
    this.records = records;
    this.campaigns = campaigns;
    this.contacts = contacts;
    this.tenants = tenants;
  }

  @Transactional
  public UsageRecord increment(UUID organizationId, boolean sent) {
    LocalDate today = LocalDate.now();
    UsageRecord record =
        records
            .findByOrganizationIdAndUsageDate(organizationId, today)
            .orElseGet(
                () -> {
                  UsageRecord created = new UsageRecord();
                  created.setOrganizationId(organizationId);
                  created.setUsageDate(today);
                  return created;
                });
    record.setEmailsAttempted(record.getEmailsAttempted() + 1);
    if (sent) {
      record.setEmailsSent(record.getEmailsSent() + 1);
    } else {
      record.setEmailsFailed(record.getEmailsFailed() + 1);
    }
    return records.saveAndFlush(record);
  }

  @Transactional(readOnly = true)
  public int emailsSentToday(UUID organizationId) {
    return records
        .findByOrganizationIdAndUsageDate(organizationId, LocalDate.now())
        .map(UsageRecord::getEmailsSent)
        .orElse(0);
  }

  @Transactional(readOnly = true)
  public PageResponse<UsageResponse> list(AuthPrincipal principal, Pageable pageable) {
    UUID orgId = principal.organizationId();
    tenants.requireMembership(principal.userId(), orgId);
    long campaignCount = campaigns.countByOrganizationId(orgId);
    long contactCount = contacts.countByOrganizationId(orgId);
    Page<UsageResponse> page =
        records
            .findByOrganizationIdOrderByUsageDateDesc(orgId, pageable)
            .map(
                record ->
                    new UsageResponse(
                        orgId,
                        record.getUsageDate(),
                        record.getEmailsAttempted(),
                        record.getEmailsSent(),
                        record.getEmailsFailed(),
                        campaignCount,
                        contactCount));
    if (page.isEmpty()) {
      UsageResponse empty =
          new UsageResponse(orgId, LocalDate.now(), 0, 0, 0, campaignCount, contactCount);
      return PageResponse.from(
          new org.springframework.data.domain.PageImpl<>(java.util.List.of(empty), pageable, 1));
    }
    return PageResponse.from(page);
  }
}
