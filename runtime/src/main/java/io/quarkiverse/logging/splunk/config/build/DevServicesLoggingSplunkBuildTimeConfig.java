package io.quarkiverse.logging.splunk.config.build;

import java.util.Map;
import java.util.Optional;

import io.smallrye.config.WithDefault;

/**
 * The build time configuration around the Splunk dev services.
 */
public interface DevServicesLoggingSplunkBuildTimeConfig {
    /**
     * whether to activate dev services or not
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * Override the docker image used for the Splunk dev service
     */
    Optional<String> imageName();

    /**
     * Whether the instance of splunk can be shared between runs in DEV mode.
     */
    @WithDefault("true")
    boolean shared();

    /**
     * Additional environment variables to inject.
     */
    Map<String, String> containerEnv();

    /**
     * Map that allows to tell to plug the following named handlers to the dev service
     * <p>
     * It is necessary as we do not have access to runtime configuration when starting the Splunk container.
     * </p>
     */
    Map<String, Boolean> plugNamedHandlers();
}
