/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configuration for Splunk HEC logging
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME, name = "log.handler.splunk")
public class SplunkConfig {

    /**
     * Determine whether to enable the handler
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * The splunk handler log level. By default it is not more strict than the root handler level.
     */
    @ConfigItem(defaultValue = "ALL")
    public Level level;

    /**
     * Splunk HEC endpoint base url.
     * <p>
     * The actual endpoint is expected at path /services/collector/events/1.0
     */
    @ConfigItem(defaultValue = "https://localhost:8088/")
    public String url;

    /**
     * Disable TLS certificate validation with HEC endpoint
     */
    @ConfigItem(defaultValue = "false")
    public boolean disableCertificateValidation;

    /**
     * The application token to authenticate with HEC, the token is mandatory if the extension is enabled
     * https://docs.splunk.com/Documentation/Splunk/8.1.0/Data/FormateventsforHTTPEventCollector#HEC_token
     */
    @ConfigItem
    public Optional<String> token;

    /**
     * The strategy to send events to HEC.
     * <p>
     * In sequential mode, there is only one HTTP connection to HEC and the order of events is preserved, but performance is
     * lower.
     * In parallel mode, event batches are sent asynchronously over multiple HTTP connections, and events with the same
     * timestamp
     * (that has 1 millisecond resolution) may be indexed out of order by Splunk.
     *
     */
    @ConfigItem(defaultValue = "sequential")
    public SendMode sendMode;

    /**
     * A GUID to identify an HEC client and guarantee isolation at HEC level in case of slow clients.
     * https://docs.splunk.com/Documentation/Splunk/8.1.0/Data/AboutHECIDXAck#About_channels_and_sending_data
     */
    @ConfigItem
    public Optional<String> channel;

    /**
     * Batching delay before sending a group of events.
     * If 0, the events are sent immediately.
     */
    @ConfigItem(defaultValue = "10s")
    public Duration batchInterval;

    /**
     * Maximum number of events in a batch. By default 10, if 0 no batching.
     */
    @ConfigItem(defaultValue = "10")
    public long batchSizeCount;

    /**
     * Maximum total size in bytes of events in a batch. By default 10KB, if 0 no batching.
     */
    @ConfigItem(defaultValue = "10")
    public long batchSizeBytes;

    /**
     * Maximum number of retries in case of I/O exceptions with HEC connection.
     */
    @ConfigItem(defaultValue = "0")
    public long maxRetries;

    /**
     * The log format, defining which metadata are inlined inside the log main payload.
     * <p>
     * Specific metadata (hostname, category, thread name, ...), as well as MDC key/value map, can also be sent in a structured
     * way.
     */
    @ConfigItem(defaultValue = "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n")
    public String format;

    /**
     * Whether to send the thrown exception message as a structured metadata of the log event (as opposed to %e in a formatted
     * message, it does not include the exception name or stacktrace)
     */
    @ConfigItem(defaultValue = "false")
    public boolean includeException;

    /**
     * Whether to send the logger name as a structured metadata of the log event (equivalent of %c in a formatted message)
     */
    @ConfigItem(defaultValue = "false")
    public boolean includeLoggerName;

    /**
     * Whether to send the thread name as a structured metadata of the log event (equivalent of %t in a formatted message)
     */
    @ConfigItem(defaultValue = "false")
    public boolean includeThreadName;

    /**
     * Overrides the host name metadata value.
     */
    @ConfigItem(defaultValueDocumentation = "The equivalent of %h in a formatted message")
    public Optional<String> metadataHost;

    /**
     * The source value to assign to the event data. For example, if you're sending data from an app you're developing,
     * you could set this key to the name of the app.
     * https://docs.splunk.com/Documentation/Splunk/8.1.0/Data/FormateventsforHTTPEventCollector#Event_metadata
     */
    @ConfigItem
    public Optional<String> metadataSource;

    /**
     * The source type value to assign to the event data
     * https://docs.splunk.com/Documentation/Splunk/8.1.0/Data/FormateventsforHTTPEventCollector#Event_metadata
     * <p>
     * A given source type may have indexed fields extraction enabled, which is the case of the default built-in _json.
     */
    @ConfigItem(defaultValue = "_json")
    public String metadataSourceType;

    /**
     * The optional name of the index by which the event data is to be stored. If set, it must be within the
     * list of allowed indexes of the token (if it has the indexes parameter set).
     * https://docs.splunk.com/Documentation/Splunk/8.1.0/Data/FormateventsforHTTPEventCollector#Event_metadata
     */
    @ConfigItem
    public Optional<String> metadataIndex;

    /**
     * Optional collection of key/value pairs to populate the "fields" key of event metadata. This key isn't
     * applicable to raw data.
     * https://docs.splunk.com/Documentation/Splunk/8.1.0/Data/FormateventsforHTTPEventCollector#Event_metadata
     */
    @ConfigItem
    public Map<String, String> metadataFields = new HashMap<>();

    /**
     * Enables the "raw" mode
     */
    @ConfigItem(defaultValue = "false")
    public boolean raw;

    /**
     * Mirrors com.splunk.logging.HttpEventCollectorSender.SendMode
     */
    public enum SendMode {
        SEQUENTIAL,
        PARALLEL
    }
}
