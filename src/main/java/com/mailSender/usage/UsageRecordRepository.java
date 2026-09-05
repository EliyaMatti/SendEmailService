package com.mailSender.usage;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageRecordRepository extends JpaRepository<UsageRecord, UUID> {

  Optional<UsageRecord> findByOrganizationIdAndUsageDate(UUID organizationId, LocalDate usageDate);

  Page<UsageRecord> findByOrganizationIdOrderByUsageDateDesc(UUID organizationId, Pageable pageable);
}
