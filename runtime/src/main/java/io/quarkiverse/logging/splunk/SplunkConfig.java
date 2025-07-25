/*
Copyright (c) 2023 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio, Yohann Puyhaubert (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
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
     * Root level.
     */
    @WithParentName
    @ConfigDocSection
    SplunkHandlerConfig config();

    /**
     * Named handlers.
     */
    @WithParentName
    @ConfigDocSection
    @ConfigDocMapKey("handler-name")
    Map<String, SplunkHandlerConfig> namedHandlers();

    /**
     * Dev Services.
     */
    @ConfigDocSection
    DevServicesLoggingSplunkRuntimeConfig devservices();
}
