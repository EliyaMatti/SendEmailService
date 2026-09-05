package com.mailSender.contact;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactListRepository extends JpaRepository<ContactList, UUID> {

  Page<ContactList> findByOrganizationId(UUID organizationId, Pageable pageable);

  Optional<ContactList> findByIdAndOrganizationId(UUID id, UUID organizationId);

  boolean existsByIdAndOrganizationId(UUID id, UUID organizationId);

  long countByOrganizationId(UUID organizationId);
}
