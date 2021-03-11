/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import java.time.Duration;
import java.util.Optional;

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
     * The log format.
     */
    @ConfigItem(defaultValue = "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n")
    String format;

    /**
     * Splunk logging input url
     */
    @ConfigItem(defaultValue = "https://localhost:8088/")
    public String url;

    /**
     * The application token, the token is mandatory if the extension is enabled
     * https://docs.splunk.com/Documentation/Splunk/8.1.0/Data/FormateventsforHTTPEventCollector#HEC_token
     */
    @ConfigItem
    public Optional<String> token;

    /**
     * To improve system performance tracing, events are sent asynchronously and
     * events with the same timestamp (that has 1 millisecond resolution) may be indexed out of order by Splunk.
     * send-mode parameter triggers "sequential mode" that guarantees preserving events order. In
     * "sequential mode" performance of sending events to the server is lower.
     */
    @ConfigItem(defaultValue = "sequential")
    public String sendMode;

    /**
     * Unique GUID for the client to send raw events to the server
     * https://docs.splunk.com/Documentation/Splunk/8.1.0/Data/AboutHECIDXAck#About_channels_and_sending_data
     */
    @ConfigItem
    public Optional<String> channel;

    /**
     * Specify if log events are sent in 'Raw' mode
     * https://docs.splunk.com/Documentation/Splunk/8.1.0/Data/FormateventsforHTTPEventCollector#Raw_event_parsing
     */
    @ConfigItem
    public Optional<String> type;

    /**
     * Batching delay before sending a group of event.
     * If 0, the events are sent immediately.
     */
    @ConfigItem(defaultValue = "10s")
    public Duration batchInterval;

    /**
     * Maximum number of events in a batch. By default 10, if 0 no batching
     */
    @ConfigItem(defaultValue = "10")
    public long batchSizeCount;

    /**
     * Maximum total size in bytes of events in a batch. By default 10KB, if 0 no batching
     */
    @ConfigItem(defaultValue = "10")
    public long batchSizeBytes;

    /**
     * Maximum error retries count
     */
    @ConfigItem(defaultValue = "0")
    public long retriesOnError;

    /**
     * Includes the exception messages when a throwable is associated
     */
    @ConfigItem(defaultValue = "true")
    public boolean includeException;

    /**
     *
     */
    @ConfigItem(defaultValue = "true")
    public boolean includeLoggerName;

    /**
     *
     */
    @ConfigItem(defaultValue = "true")
    public boolean includeThreadName;

    /**
     * Disable certificate validation
     */
    @ConfigItem(defaultValue = "false")
    public boolean disableCertificateValidation;

    /**
     * The host value to assign to the event data. This is typically the hostname of
     * the client from which you're sending data.
     * https://docs.splunk.com/Documentation/Splunk/8.1.0/Data/FormateventsforHTTPEventCollector#Event_metadata
     */
    @ConfigItem
    public Optional<String> metadataHost;

    /**
     * The source value to assign to the event data. For example, if you're sending data from an app you're developing,
     * you could set this key to the name of the app.
     * https://docs.splunk.com/Documentation/Splunk/8.1.0/Data/FormateventsforHTTPEventCollector#Event_metadata
     */
    @ConfigItem
    public Optional<String> metadataSource;

    /**
     * The sourcetype value to assign to the event data
     * https://docs.splunk.com/Documentation/Splunk/8.1.0/Data/FormateventsforHTTPEventCollector#Event_metadata
     *
     * A given sourcetype may have indexed fields extraction enabled, which is the case of the default built-in _json.
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
}
