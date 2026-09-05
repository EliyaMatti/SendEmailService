package com.mailSender.smtpaccount;

import com.mailSender.common.response.ApiResponse;
import com.mailSender.common.response.PageResponse;
import com.mailSender.common.security.AuthPrincipal;
import com.mailSender.config.ApplicationProfiles;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile(ApplicationProfiles.API)
@RequestMapping("/api/v1/smtp")
public class SmtpAccountController {

  private final SmtpAccountService smtpAccounts;

  public SmtpAccountController(SmtpAccountService smtpAccounts) {
    this.smtpAccounts = smtpAccounts;
  }

  @PostMapping
  public ApiResponse<SmtpAccountResponse> create(
      @AuthenticationPrincipal AuthPrincipal principal,
      @Valid @RequestBody CreateSmtpAccountRequest request) {
    return ApiResponse.ok(smtpAccounts.create(principal, request));
  }

  @GetMapping
  public ApiResponse<PageResponse<SmtpAccountResponse>> list(
      @AuthenticationPrincipal AuthPrincipal principal, @PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.ok(smtpAccounts.list(principal, pageable));
  }

  @GetMapping("/{id}")
  public ApiResponse<SmtpAccountResponse> get(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
    return ApiResponse.ok(smtpAccounts.get(principal, id));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
    smtpAccounts.delete(principal, id);
    return ApiResponse.ok(null);
  }

  @PostMapping("/{id}/test")
  public ApiResponse<SmtpTestResponse> test(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
    return ApiResponse.ok(smtpAccounts.test(principal, id));
  }
}
