package com.mailSender.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@Profile(ApplicationProfiles.API)
@EnableTransactionManagement
@EntityScan(basePackages = "com.mailSender")
@EnableJpaRepositories(basePackages = "com.mailSender")
public class ApiPersistenceConfig {}
