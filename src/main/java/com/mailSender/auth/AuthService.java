package com.mailSender.auth;

import com.mailSender.common.exception.ApiException;
import com.mailSender.common.security.AuthPrincipal;
import com.mailSender.common.security.AuthRateLimiter;
import com.mailSender.common.security.JwtService;
import com.mailSender.config.ApplicationProfiles;
import com.mailSender.config.ExcelmailProperties;
import com.mailSender.organization.MemberRole;
import com.mailSender.organization.Organization;
import com.mailSender.organization.OrganizationMember;
import com.mailSender.organization.OrganizationMemberRepository;
import com.mailSender.organization.OrganizationRepository;
import com.mailSender.user.UserAccount;
import com.mailSender.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile(ApplicationProfiles.API)
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UserAccountRepository users;
  private final OrganizationRepository organizations;
  private final OrganizationMemberRepository members;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthRateLimiter rateLimiter;
  private final ExcelmailProperties properties;

  public AuthService(
      UserAccountRepository users,
      OrganizationRepository organizations,
      OrganizationMemberRepository members,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      AuthRateLimiter rateLimiter,
      ExcelmailProperties properties) {
    this.users = users;
    this.organizations = organizations;
    this.members = members;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.rateLimiter = rateLimiter;
    this.properties = properties;
  }

  @Transactional
  public AuthTokenResponse register(RegisterRequest request, String clientKey) {
    rateLimiter.check("register:" + clientKey, properties.getAuth().getRateLimitPerMinute());
    String email = request.getEmail().trim().toLowerCase();
    if (users.existsByEmailIgnoreCase(email)) {
      throw new ApiException("DUPLICATE_EMAIL", "An account with that email already exists.", 409);
    }
    UserAccount user = new UserAccount();
    user.setEmail(email);
    user.setName(request.getName().trim());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    users.saveAndFlush(user);

    Organization organization = new Organization();
    organization.setName(user.getName() + "'s organization");
    organization.setOwnerId(user.getId());
    organizations.saveAndFlush(organization);

    OrganizationMember member = new OrganizationMember();
    member.setOrganizationId(organization.getId());
    member.setUserId(user.getId());
    member.setRole(MemberRole.OWNER);
    members.saveAndFlush(member);

    log.info("User registered");
    return tokenResponse(user, organization.getId(), MemberRole.OWNER.name());
  }

  @Transactional(readOnly = true)
  public AuthTokenResponse login(LoginRequest request, String clientKey) {
    rateLimiter.check("login:" + clientKey, properties.getAuth().getRateLimitPerMinute());
    UserAccount user =
        users
            .findByEmailIgnoreCase(request.getEmail().trim())
            .orElseThrow(() -> invalidLogin());
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw invalidLogin();
    }
    OrganizationMember membership =
        members.findByUserId(user.getId()).stream()
            .findFirst()
            .orElseThrow(
                () ->
                    new ApiException(
                        "ORGANIZATION_NOT_FOUND",
                        "No organization is linked to this account.",
                        404));
    log.info("User authenticated");
    return tokenResponse(user, membership.getOrganizationId(), membership.getRole().name());
  }

  @Transactional(readOnly = true)
  public AuthTokenResponse.UserResponse me(AuthPrincipal principal) {
    UserAccount user =
        users
            .findById(principal.userId())
            .orElseThrow(
                () -> new ApiException("UNAUTHENTICATED", "Authentication is required.", 401));
    return new AuthTokenResponse.UserResponse(
        user.getId(),
        user.getName(),
        user.getEmail(),
        principal.organizationId(),
        principal.role());
  }

  private AuthTokenResponse tokenResponse(UserAccount user, java.util.UUID organizationId, String role) {
    AuthPrincipal principal =
        new AuthPrincipal(user.getId(), organizationId, user.getEmail(), role);
    return new AuthTokenResponse(
        jwtService.createToken(principal),
        new AuthTokenResponse.UserResponse(
            user.getId(), user.getName(), user.getEmail(), organizationId, role));
  }

  private static ApiException invalidLogin() {
    return new ApiException("INVALID_CREDENTIALS", "Email or password is incorrect.", 401);
  }
}
