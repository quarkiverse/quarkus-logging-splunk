package io.quarkiverse.logging.splunk;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class DevServicesLoggingSplunkRuntimeConfig {
    /**
     * The API URL the splunk dev service listens on.
     */
    @ConfigItem
    public Optional<String> apiUrl;
}
