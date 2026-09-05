package com.mailSender.mailtemplate;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredEmailTemplateRepository extends JpaRepository<StoredEmailTemplate, UUID> {

  Page<StoredEmailTemplate> findByOrganizationId(UUID organizationId, Pageable pageable);

  Optional<StoredEmailTemplate> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
