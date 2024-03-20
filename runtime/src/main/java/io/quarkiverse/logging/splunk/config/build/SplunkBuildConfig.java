package io.quarkiverse.logging.splunk.config.build;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * The build time configuration for the Splunk logging extension.
 */
@ConfigRoot(name = "log.handler.splunk", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class SplunkBuildConfig {
    /**
     * Configuration for the dev services.
     */
    @ConfigItem
    public DevServicesLoggingSplunkBuildTimeConfig devservices;
}
