package org.alexstpm.fmsstartuplogger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class StartupLoggerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(StartupLoggerAutoConfiguration.class));

    @Test
    void createsLoggerByDefault() {
        // given / when
        contextRunner.run(context -> {
            // then
            assertThat(context).hasSingleBean(StartupInfoLogger.class);
            assertThat(context).hasBean("startupInfoListener");
        });
    }

    @Test
    void doesNotCreateLoggerWhenDisabled() {
        // given / when
        contextRunner.withPropertyValues("app.startup-logger.enabled=false").run(context -> {
            // then
            assertThat(context).doesNotHaveBean(StartupInfoLogger.class);
            assertThat(context).doesNotHaveBean("startupInfoListener");
        });
    }
}
