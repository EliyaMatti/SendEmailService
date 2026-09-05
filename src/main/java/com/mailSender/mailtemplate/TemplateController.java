package com.mailSender.mailtemplate;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile(ApplicationProfiles.API)
@RequestMapping("/api/v1/templates")
public class TemplateController {

  private final TemplateService templates;

  public TemplateController(TemplateService templates) {
    this.templates = templates;
  }

  @PostMapping
  public ApiResponse<TemplateResponse> create(
      @AuthenticationPrincipal AuthPrincipal principal,
      @Valid @RequestBody UpsertTemplateRequest request) {
    return ApiResponse.ok(templates.create(principal, request));
  }

  @GetMapping
  public ApiResponse<PageResponse<TemplateResponse>> list(
      @AuthenticationPrincipal AuthPrincipal principal, @PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.ok(templates.list(principal, pageable));
  }

  @GetMapping("/{id}")
  public ApiResponse<TemplateResponse> get(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
    return ApiResponse.ok(templates.get(principal, id));
  }

  @PutMapping("/{id}")
  public ApiResponse<TemplateResponse> update(
      @AuthenticationPrincipal AuthPrincipal principal,
      @PathVariable UUID id,
      @Valid @RequestBody UpsertTemplateRequest request) {
    return ApiResponse.ok(templates.update(principal, id, request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
    templates.delete(principal, id);
    return ApiResponse.ok(null);
  }
}
