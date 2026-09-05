package com.mailSender.smtpaccount;

import com.mailSender.config.ApplicationProfiles;
import com.mailSender.config.SmtpConfiguration;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import java.util.Properties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile(ApplicationProfiles.API)
public class JavaMailSmtpConnectionTester implements SmtpConnectionTester {

  @Override
  public void test(SmtpConfiguration configuration) throws Exception {
    Properties props = new Properties();
    props.put("mail.smtp.auth", String.valueOf(configuration.isAuthEnabled()));
    props.put("mail.smtp.starttls.enable", String.valueOf(configuration.isTlsEnabled()));
    props.put("mail.smtp.host", configuration.getHost());
    props.put("mail.smtp.port", String.valueOf(configuration.getPort()));
    props.put("mail.smtp.connectiontimeout", "5000");
    props.put("mail.smtp.timeout", "5000");
    Session session = Session.getInstance(props);
    try (Transport transport = session.getTransport("smtp")) {
      transport.connect(
          configuration.getHost(),
          configuration.getPort(),
          configuration.getUsername(),
          configuration.getPassword());
    }
  }
}
