package com.mailSender.campaign;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

  Page<Campaign> findByOrganizationId(UUID organizationId, Pageable pageable);

  Optional<Campaign> findByIdAndOrganizationId(UUID id, UUID organizationId);

  long countByOrganizationId(UUID organizationId);
}
