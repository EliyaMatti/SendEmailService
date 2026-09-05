package com.mailSender.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;

class ApplicationProfilesTest {

  @Test
  void cliDefaultIsNotApi() {
    assertFalse(ApplicationProfiles.apiRequested(new String[0], null));
    assertEquals(WebApplicationType.NONE, ApplicationProfiles.webApplicationType(false));
  }

  @Test
  void apiProfileFromEnv() {
    assertTrue(ApplicationProfiles.apiRequested(new String[0], "api"));
    assertTrue(ApplicationProfiles.apiRequested(new String[0], "development,api"));
    assertFalse(ApplicationProfiles.apiRequested(new String[0], "development"));
    assertEquals(WebApplicationType.SERVLET, ApplicationProfiles.webApplicationType(true));
  }

  @Test
  void apiProfileFromArgs() {
    assertTrue(
        ApplicationProfiles.apiRequested(new String[] {"--spring.profiles.active=api"}, null));
    assertTrue(
        ApplicationProfiles.apiRequested(
            new String[] {"--spring.profiles.active=development,api"}, null));
    assertFalse(
        ApplicationProfiles.apiRequested(
            new String[] {"--spring.profiles.active=development"}, null));
  }
}
