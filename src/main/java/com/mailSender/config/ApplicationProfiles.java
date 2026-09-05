package com.mailSender.config;

import org.springframework.boot.WebApplicationType;

/**
 * Dual run modes (M2-003): CLI stays {@link WebApplicationType#NONE}; the {@code api} profile uses
 * a servlet container. Other profiles ({@code development}, {@code production}) do not enable HTTP.
 */
public final class ApplicationProfiles {

  public static final String API = "api";

  private ApplicationProfiles() {}

  public static boolean apiRequested(String[] args, String springProfilesActiveEnv) {
    if (containsApiProfile(springProfilesActiveEnv)) {
      return true;
    }
    if (args == null) {
      return false;
    }
    for (String arg : args) {
      if (arg == null) {
        continue;
      }
      if (arg.startsWith("--spring.profiles.active=")) {
        if (containsApiProfile(arg.substring("--spring.profiles.active=".length()))) {
          return true;
        }
      }
      if (arg.startsWith("--spring.profiles.active")) {
        int eq = arg.indexOf('=');
        if (eq > 0 && containsApiProfile(arg.substring(eq + 1))) {
          return true;
        }
      }
    }
    return false;
  }

  public static WebApplicationType webApplicationType(boolean api) {
    return api ? WebApplicationType.SERVLET : WebApplicationType.NONE;
  }

  static boolean containsApiProfile(String profiles) {
    if (profiles == null || profiles.isBlank()) {
      return false;
    }
    for (String profile : profiles.split(",")) {
      if (API.equals(profile.trim())) {
        return true;
      }
    }
    return false;
  }
}
