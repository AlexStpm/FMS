package org.alexstpm.fmsstartuplogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;

public class StartupInfoLogger {

    private static final Logger log =
            LoggerFactory.getLogger(StartupInfoLogger.class);

    private final Environment environment;
    private final ObjectProvider<BuildProperties> buildProperties;

    public StartupInfoLogger(
            Environment environment,
            ObjectProvider<BuildProperties> buildProperties
    ) {
        this.environment = environment;
        this.buildProperties = buildProperties;
    }

    public void log(ApplicationReadyEvent event) {
        String[] activeProfiles = environment.getActiveProfiles();

        String profiles = activeProfiles.length == 0
                ? "default"
                : String.join(",", activeProfiles);

        BuildProperties build = buildProperties.getIfAvailable();

        String version = build != null
                ? build.getVersion()
                : environment.getProperty("spring.application.version", "unknown");

        String datasourceUrl = environment.getProperty("spring.datasource.url");
        String safeDatasourceUrl = sanitizeJdbcUrl(datasourceUrl);

        Duration timeTaken = event.getTimeTaken();
        String startupDuration = timeTaken == null
                ? "unknown"
                : timeTaken.toMillis() + "ms";

        log.info(
                "Application ready: profiles={}, version={}, datasource={}, startupDuration={}",
                profiles,
                version,
                safeDatasourceUrl,
                startupDuration
        );
    }

    private String sanitizeJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return "not configured";
        }
        String withoutQuery = jdbcUrl.split("\\?", 2)[0];
        return withoutQuery.replaceFirst("//[^/@]+@", "//");
    }
}
