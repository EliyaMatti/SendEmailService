package com.mailSender;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MailAppProperties.class)
public class MailSenderApplication {

  public static void main(String[] args) {
    SpringApplication.run(MailSenderApplication.class, args);
  }
}
