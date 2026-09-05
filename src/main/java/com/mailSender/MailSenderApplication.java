package com.mailSender;

import com.mailSender.config.ApplicationProfiles;
import com.mailSender.config.MailAppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({MailAppProperties.class, MailProperties.class})
public class MailSenderApplication {

  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(MailSenderApplication.class);
    application.setWebApplicationType(resolveWebApplicationType(args));
    application.run(args);
  }

  static WebApplicationType resolveWebApplicationType(String[] args) {
    boolean api =
        ApplicationProfiles.apiRequested(args, System.getenv("SPRING_PROFILES_ACTIVE"));
    return ApplicationProfiles.webApplicationType(api);
  }
}
