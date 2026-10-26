package org.alexstpm.fmsstartuplogger;

import org.alexstpm.fmsstartuplogger.StartupInfoLogger;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.ObjectProvider;

@AutoConfiguration
@ConditionalOnClass(ApplicationReadyEvent.class)
@ConditionalOnProperty(
        prefix = "app.startup-logger",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class StartupLoggerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    StartupInfoLogger startupInfoLogger(
            Environment environment,
            ObjectProvider<BuildProperties> buildProperties
    ) {
        return new StartupInfoLogger(environment, buildProperties);
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> startupInfoListener(
            StartupInfoLogger startupInfoLogger
    ) {
        return startupInfoLogger::log;
    }
}
