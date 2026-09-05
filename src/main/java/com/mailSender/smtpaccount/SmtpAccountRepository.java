package com.mailSender.smtpaccount;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmtpAccountRepository extends JpaRepository<SmtpAccount, UUID> {

  Page<SmtpAccount> findByOrganizationId(UUID organizationId, Pageable pageable);

  Optional<SmtpAccount> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
