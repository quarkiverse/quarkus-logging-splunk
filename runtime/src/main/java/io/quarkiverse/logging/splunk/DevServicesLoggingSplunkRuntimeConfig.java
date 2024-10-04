package io.quarkiverse.logging.splunk;

import java.util.Optional;

public interface DevServicesLoggingSplunkRuntimeConfig {
    /**
     * The API URL the splunk dev service listens on.
     */
    Optional<String> apiUrl();
}
