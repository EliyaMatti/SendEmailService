package com.mailSender.organization;

import com.mailSender.common.exception.ApiException;
import com.mailSender.config.ApplicationProfiles;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile(ApplicationProfiles.API)
public class TenantService {

  private final OrganizationMemberRepository members;
  private final OrganizationRepository organizations;

  public TenantService(
      OrganizationMemberRepository members, OrganizationRepository organizations) {
    this.members = members;
    this.organizations = organizations;
  }

  @Transactional(readOnly = true)
  public OrganizationMember requireMembership(UUID userId, UUID organizationId) {
    return members
        .findByOrganizationIdAndUserId(organizationId, userId)
        .orElseThrow(
            () ->
                new ApiException(
                    "ORGANIZATION_ACCESS_DENIED",
                    "You do not have access to this organization.",
                    403));
  }

  @Transactional(readOnly = true)
  public void requireOwnedOrganization(UUID organizationId) {
    if (!organizations.existsById(organizationId)) {
      throw new ApiException(
          "ORGANIZATION_NOT_FOUND", "The requested organization was not found.", 404);
    }
  }
}
