package io.quarkiverse.logging.splunk.config.build;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * The build time configuration for the Splunk logging extension.
 */
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "quarkus.log.handler.splunk")
public interface SplunkBuildConfig {
    /**
     * Dev Services.
     */
    @ConfigDocSection
    DevServicesLoggingSplunkBuildTimeConfig devservices();
}
