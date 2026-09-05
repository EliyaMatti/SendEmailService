package com.mailSender.campaign;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile(ApplicationProfiles.API)
@RequestMapping("/api/v1/campaigns")
public class CampaignController {

  private final CampaignService campaigns;

  public CampaignController(CampaignService campaigns) {
    this.campaigns = campaigns;
  }

  @PostMapping
  public ApiResponse<CampaignResponse> create(
      @AuthenticationPrincipal AuthPrincipal principal,
      @Valid @RequestBody CreateCampaignRequest request) {
    return ApiResponse.ok(campaigns.create(principal, request));
  }

  @GetMapping
  public ApiResponse<PageResponse<CampaignResponse>> list(
      @AuthenticationPrincipal AuthPrincipal principal, @PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.ok(campaigns.list(principal, pageable));
  }

  @GetMapping("/{id}")
  public ApiResponse<CampaignResponse> get(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
    return ApiResponse.ok(campaigns.get(principal, id));
  }

  @PostMapping("/{id}/start")
  public ApiResponse<CampaignResponse> start(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
    return ApiResponse.ok(campaigns.start(principal, id));
  }

  @PostMapping("/{id}/pause")
  public ApiResponse<CampaignResponse> pause(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
    return ApiResponse.ok(campaigns.pause(principal, id));
  }

  @PostMapping("/{id}/resume")
  public ApiResponse<CampaignResponse> resume(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
    return ApiResponse.ok(campaigns.resume(principal, id));
  }

  @PostMapping("/{id}/cancel")
  public ApiResponse<CampaignResponse> cancel(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
    return ApiResponse.ok(campaigns.cancel(principal, id));
  }
}
