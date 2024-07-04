package io.quarkiverse.logging.splunk.config.build;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * The build time configuration around the Splunk dev services.
 */
@ConfigGroup
public class DevServicesLoggingSplunkBuildTimeConfig {
    /**
     * whether to activate dev services or not
     */
    @ConfigItem
    public Optional<Boolean> enabled = Optional.empty();

    /**
     * Override the docker image used for the Splunk dev service
     */
    @ConfigItem
    public Optional<String> imageName;

    /**
     * Whether the instance of splunk can be shared between runs in DEV mode.
     */
    @ConfigItem(defaultValue = "true")
    public boolean shared;

    /**
     * Additional environment variables to inject.
     */
    @ConfigItem
    public Map<String, String> containerEnv;

    /**
     * Map that allows to tell to plug the following named handlers to the dev service
     * <p>
     * It is necessary as we do not have access to runtime configuration when starting the Splunk container.
     * </p>
     */
    @ConfigItem
    public Map<String, Boolean> plugNamedHandlers;
}
