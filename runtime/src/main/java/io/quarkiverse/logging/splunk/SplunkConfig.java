/*
Copyright (c) 2023 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio, Yohann Puyhaubert (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configuration for Splunk HEC logging
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME, name = "log.handler.splunk")
public class SplunkConfig {

    /**
     * Configuration for Splunk HEC logging for the root level.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public SplunkHandlerConfig config;
    /**
     * Map of all the custom/named handlers configuration using Splunk implementation.
     */
    @ConfigItem(name = ConfigItem.PARENT)
    public Map<String, SplunkHandlerConfig> namedHandlers;

    public DevServicesLoggingSplunkRuntimeConfig devservices;
}
