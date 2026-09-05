package com.mailSender.campaign;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CampaignRecipientRepository extends JpaRepository<CampaignRecipient, UUID> {

  List<CampaignRecipient> findByCampaignId(UUID campaignId);

  long countByCampaignIdAndStatus(UUID campaignId, RecipientStatus status);

  long countByCampaign_OrganizationIdAndStatusAndSentAtAfter(
      UUID organizationId, RecipientStatus status, Instant sentAfter);

  @Query(
      """
      SELECT r FROM CampaignRecipient r
      JOIN FETCH r.campaign c
      WHERE r.status = com.mailSender.campaign.RecipientStatus.PENDING
        AND c.status = com.mailSender.campaign.CampaignStatus.RUNNING
      ORDER BY r.queuedAt ASC
      """)
  List<CampaignRecipient> findClaimable(Pageable pageable);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          "UPDATE campaign_recipients SET status = 'PROCESSING', updated_at = CURRENT_TIMESTAMP WHERE id = :id AND status = 'PENDING'",
      nativeQuery = true)
  int claimPending(@Param("id") UUID id);

  Optional<CampaignRecipient> findByIdAndCampaign_OrganizationId(UUID id, UUID organizationId);
}
