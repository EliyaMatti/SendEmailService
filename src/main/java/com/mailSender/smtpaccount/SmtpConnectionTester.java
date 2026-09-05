package com.mailSender.smtpaccount;

import com.mailSender.config.SmtpConfiguration;

public interface SmtpConnectionTester {

  void test(SmtpConfiguration configuration) throws Exception;
}
