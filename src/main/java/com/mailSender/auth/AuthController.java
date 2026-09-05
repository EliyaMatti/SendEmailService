package com.mailSender.auth;

import com.mailSender.common.response.ApiResponse;
import com.mailSender.common.security.AuthPrincipal;
import com.mailSender.config.ApplicationProfiles;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile(ApplicationProfiles.API)
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public ApiResponse<AuthTokenResponse> register(
      @Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
    return ApiResponse.ok(authService.register(request, clientKey(http)));
  }

  @PostMapping("/login")
  public ApiResponse<AuthTokenResponse> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest http) {
    return ApiResponse.ok(authService.login(request, clientKey(http)));
  }

  @GetMapping("/me")
  public ApiResponse<AuthTokenResponse.UserResponse> me(
      @AuthenticationPrincipal AuthPrincipal principal) {
    return ApiResponse.ok(authService.me(principal));
  }

  private static String clientKey(HttpServletRequest http) {
    String forwarded = http.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return http.getRemoteAddr();
  }
}
