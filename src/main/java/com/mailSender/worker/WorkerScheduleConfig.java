package com.mailSender.worker;

import com.mailSender.campaign.EmailComposer;
import com.mailSender.config.ApplicationProfiles;
import com.mailSender.config.MailAppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@Profile(ApplicationProfiles.API)
@EnableScheduling
public class WorkerScheduleConfig {

  @Bean
  public EmailComposer emailComposer(MailAppProperties mailAppProperties) {
    return new EmailComposer(mailAppProperties);
  }
}
