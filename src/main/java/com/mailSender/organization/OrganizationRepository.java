package com.mailSender.organization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

  List<Organization> findByOwnerId(UUID ownerId);

  Optional<Organization> findByIdAndOwnerId(UUID id, UUID ownerId);
}
