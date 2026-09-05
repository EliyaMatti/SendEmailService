package com.mailSender.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile(ApplicationProfiles.API)
public class OpenApiConfig {

  @Bean
  public OpenAPI excelMailOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("ExcelMail Pro API")
                .version("v1")
                .description(
                    "SaaS backend for contact import, templates, SMTP accounts, campaigns, and usage. SMTP passwords are write-only."));
  }
}
