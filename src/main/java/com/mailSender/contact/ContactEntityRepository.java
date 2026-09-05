package com.mailSender.contact;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactEntityRepository extends JpaRepository<ContactEntity, UUID> {

  Page<ContactEntity> findByOrganizationIdAndContactListId(
      UUID organizationId, UUID contactListId, Pageable pageable);

  List<ContactEntity> findByOrganizationIdAndContactListId(UUID organizationId, UUID contactListId);

  long countByOrganizationIdAndContactListId(UUID organizationId, UUID contactListId);

  long countByOrganizationId(UUID organizationId);

  void deleteByContactListIdAndOrganizationId(UUID contactListId, UUID organizationId);
}
