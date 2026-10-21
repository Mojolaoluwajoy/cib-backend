package org.app.corporateinternetbanking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

// Background jobs run in the server only, never during one-off commands such as create-super-admin.
@Configuration
@EnableScheduling
@Profile("!cli")
public class SchedulingConfig {}
