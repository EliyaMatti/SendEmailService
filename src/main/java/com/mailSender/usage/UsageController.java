package com.mailSender.usage;

import com.mailSender.common.response.ApiResponse;
import com.mailSender.common.response.PageResponse;
import com.mailSender.common.security.AuthPrincipal;
import com.mailSender.config.ApplicationProfiles;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile(ApplicationProfiles.API)
@RequestMapping("/api/v1/usage")
public class UsageController {

  private final UsageService usage;

  public UsageController(UsageService usage) {
    this.usage = usage;
  }

  @GetMapping
  public ApiResponse<PageResponse<UsageResponse>> list(
      @AuthenticationPrincipal AuthPrincipal principal, @PageableDefault(size = 30) Pageable pageable) {
    return ApiResponse.ok(usage.list(principal, pageable));
  }
}
