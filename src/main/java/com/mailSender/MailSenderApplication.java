package com.mailSender;

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
    application.setWebApplicationType(WebApplicationType.NONE);
    application.run(args);
  }
}
