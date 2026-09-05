package com.mailSender.organization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

  Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

  List<OrganizationMember> findByUserId(UUID userId);

  boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);
}
