package com.mailSender.smtpaccount;

import com.mailSender.common.exception.ApiException;
import com.mailSender.common.response.PageResponse;
import com.mailSender.common.security.AuthPrincipal;
import com.mailSender.config.ApplicationProfiles;
import com.mailSender.config.SmtpConfiguration;
import com.mailSender.organization.TenantService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile(ApplicationProfiles.API)
public class SmtpAccountService {

  private static final Logger log = LoggerFactory.getLogger(SmtpAccountService.class);

  private final SmtpAccountRepository accounts;
  private final TenantService tenants;
  private final AesGcmEncryptor encryptor;
  private final SmtpConnectionTester tester;

  public SmtpAccountService(
      SmtpAccountRepository accounts,
      TenantService tenants,
      AesGcmEncryptor encryptor,
      SmtpConnectionTester tester) {
    this.accounts = accounts;
    this.tenants = tenants;
    this.encryptor = encryptor;
    this.tester = tester;
  }

  @Transactional
  public SmtpAccountResponse create(AuthPrincipal principal, CreateSmtpAccountRequest request) {
    UUID orgId = org(principal);
    SmtpAccount account = new SmtpAccount();
    account.setOrganizationId(orgId);
    account.setProvider(request.getProvider().trim());
    account.setHost(request.getHost().trim());
    account.setPort(request.getPort());
    account.setUsername(request.getUsername().trim());
    account.setEncryptedPassword(encryptor.encrypt(request.getPassword()));
    account.setKeyVersion(encryptor.currentKeyVersion());
    account.setFromEmail(request.getFromEmail().trim());
    account.setFromName(request.getFromName() == null ? "" : request.getFromName().trim());
    account.setTlsEnabled(request.isTlsEnabled());
    accounts.saveAndFlush(account);
    log.info("SMTP account stored");
    return toResponse(account);
  }

  @Transactional(readOnly = true)
  public PageResponse<SmtpAccountResponse> list(AuthPrincipal principal, Pageable pageable) {
    return PageResponse.from(accounts.findByOrganizationId(org(principal), pageable).map(this::toResponse));
  }

  @Transactional(readOnly = true)
  public SmtpAccountResponse get(AuthPrincipal principal, UUID id) {
    return toResponse(require(principal, id));
  }

  @Transactional
  public void delete(AuthPrincipal principal, UUID id) {
    accounts.delete(require(principal, id));
    log.info("SMTP account deleted");
  }

  @Transactional(readOnly = true)
  public SmtpTestResponse test(AuthPrincipal principal, UUID id) {
    SmtpAccount account = require(principal, id);
    SmtpConfiguration configuration = toSmtpConfiguration(account);
    try {
      tester.test(configuration);
      log.info("SMTP connection test succeeded");
      return new SmtpTestResponse(true, "SMTP connection succeeded.");
    } catch (Exception e) {
      log.info("SMTP connection test failed");
      return new SmtpTestResponse(false, "SMTP connection failed. Check host, port, TLS, and credentials.");
    }
  }

  public SmtpAccount require(AuthPrincipal principal, UUID id) {
    return accounts
        .findByIdAndOrganizationId(id, org(principal))
        .orElseThrow(
            () -> new ApiException("SMTP_ACCOUNT_NOT_FOUND", "The SMTP account was not found.", 404));
  }

  public SmtpConfiguration toSmtpConfiguration(SmtpAccount account) {
    String password = encryptor.decrypt(account.getEncryptedPassword(), account.getKeyVersion());
    return new SmtpConfiguration(
        account.getHost(),
        account.getPort(),
        account.getUsername(),
        password,
        account.getFromEmail(),
        account.getFromName() == null ? "" : account.getFromName(),
        account.isTlsEnabled());
  }

  private SmtpAccountResponse toResponse(SmtpAccount account) {
    return new SmtpAccountResponse(
        account.getId(),
        account.getOrganizationId(),
        account.getProvider(),
        account.getHost(),
        account.getPort(),
        account.getUsername(),
        account.getFromEmail(),
        account.getFromName(),
        account.isTlsEnabled(),
        account.getCreatedAt(),
        account.getUpdatedAt());
  }

  private UUID org(AuthPrincipal principal) {
    tenants.requireMembership(principal.userId(), principal.organizationId());
    return principal.organizationId();
  }
}
