/*
Copyright (c) 2023 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio, Yohann Puyhaubert (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

/**
 * Configuration for Splunk HEC logging
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.log.handler.splunk")
public interface SplunkConfig {

    /**
     * Configuration for Splunk HEC logging for the root level.
     */
    @WithParentName
    SplunkHandlerConfig config();

    /**
     * Map of all the custom/named handlers configuration using Splunk implementation.
     */
    @WithParentName
    Map<String, SplunkHandlerConfig> namedHandlers();

    /**
     * Runtime configuration for the Splunk DevService.
     */
    DevServicesLoggingSplunkRuntimeConfig devservices();
}
