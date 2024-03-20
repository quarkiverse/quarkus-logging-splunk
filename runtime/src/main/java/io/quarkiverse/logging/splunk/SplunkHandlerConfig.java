/*
Copyright (c) 2023 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio, Yohann Puyhaubert (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * The configuration of the Splunk root or any Splunk named handler.
 */
@ConfigGroup
public class SplunkHandlerConfig {
    /**
     * Determine whether to enable the handler
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled = true;

    public static class Enabled implements BooleanSupplier {

        final SplunkHandlerConfig config;

        public Enabled(SplunkHandlerConfig config) {
            this.config = config;
        }

        @Override
        public boolean getAsBoolean() {
            return config.enabled;
        }
    }

    /**
     * The splunk handler log level. By default, it is no more strict than the root handler level.
     */
    @ConfigItem(defaultValue = "ALL")
    public Level level = Level.ALL;

    /**
     * Splunk HEC endpoint base url.
     * <p>
     * With raw events, the endpoint targeted is /services/collector/raw.
     * With flat or nested JSON events, the endpoint targeted is /services/collector/event/1.0.
     */
    @ConfigItem(defaultValue = "https://localhost:8088/")
    public String url = "https://localhost:8088/";

    /**
     * Disable TLS certificate validation with HEC endpoint
     */
    @ConfigItem(defaultValue = "false")
    public boolean disableCertificateValidation = false;

    /**
     * The application token to authenticate with HEC, the token is mandatory if the extension is enabled
     * https://docs.splunk.com/Documentation/Splunk/latest/Data/FormateventsforHTTPEventCollector#HEC_token
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
     */
    @ConfigItem(defaultValue = "sequential")
    public SendMode sendMode = SendMode.SEQUENTIAL;

    /**
     * A GUID to identify an HEC client and guarantee isolation at HEC level in case of slow clients.
     * https://docs.splunk.com/Documentation/Splunk/latest/Data/AboutHECIDXAck#About_channels_and_sending_data
     */
    @ConfigItem
    public Optional<String> channel;

    /**
     * Batching delay before sending a group of events.
     * If 0, the events are sent immediately.
     */
    @ConfigItem(defaultValue = "10s")
    public Duration batchInterval = Duration.ofSeconds(10);

    /**
     * Maximum number of events in a batch. By default 10, if 0 no batching.
     */
    @ConfigItem(defaultValue = "10")
    public long batchSizeCount = 10;

    /**
     * Maximum total size in bytes of events in a batch. By default 10KB, if 0 no batching.
     */
    @ConfigItem(defaultValue = "10240")
    public long batchSizeBytes = 10 * 1024;

    /**
     * Maximum number of retries in case of I/O exceptions with HEC connection.
     */
    @ConfigItem(defaultValue = "0")
    public long maxRetries = 0;

    /**
     * The log format, defining which metadata are inlined inside the log main payload.
     * <p>
     * Specific metadata (hostname, category, thread name, ...), as well as MDC key/value map, can also be sent in a structured
     * way.
     */
    @ConfigItem(defaultValue = "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n")
    public String format = "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n";

    /**
     * Whether to send the thrown exception message as a structured metadata of the log event (as opposed to %e in a formatted
     * message, it does not include the exception name or stacktrace).
     * Only applicable to 'nested' serialization.
     */
    @ConfigItem(defaultValue = "false")
    public boolean includeException = false;

    /**
     * Whether to send the logger name as a structured metadata of the log event (equivalent of %c in a formatted message).
     * Only applicable to 'nested' serialization.
     */
    @ConfigItem(defaultValue = "false")
    public boolean includeLoggerName = false;

    /**
     * Whether to send the thread name as a structured metadata of the log event (equivalent of %t in a formatted message).
     * Only applicable to 'nested' serialization.
     */
    @ConfigItem(defaultValue = "false")
    public boolean includeThreadName = false;

    /**
     * Overrides the host name metadata value.
     */
    @ConfigItem(defaultValueDocumentation = "The equivalent of %h in a formatted message")
    public Optional<String> metadataHost;

    /**
     * The source value to assign to the event data. For example, if you're sending data from an app you're developing,
     * you could set this key to the name of the app.
     * https://docs.splunk.com/Documentation/Splunk/latest/Data/FormateventsforHTTPEventCollector#Event_metadata
     */
    @ConfigItem
    public Optional<String> metadataSource;

    /**
     * The optional format of the events, to enable some parsing on Splunk side.
     * https://docs.splunk.com/Documentation/Splunk/latest/Data/FormateventsforHTTPEventCollector#Event_metadata
     * <p>
     * A given source type may have indexed fields extraction enabled, which is the case of the built-in _json used for nested
     * serialization.
     */
    @ConfigItem(defaultValueDocumentation = "_json for nested serialization, not set otherwise")
    public Optional<String> metadataSourceType;

    /**
     * The optional name of the index by which the event data is to be stored. If set, it must be within the
     * list of allowed indexes of the token (if it has the indexes parameter set).
     * https://docs.splunk.com/Documentation/Splunk/latest/Data/FormateventsforHTTPEventCollector#Event_metadata
     */
    @ConfigItem
    public Optional<String> metadataIndex;

    /**
     * Optional static key/value pairs to populate the "fields" key of event metadata. This isn't
     * applicable to raw serialization.
     * https://docs.splunk.com/Documentation/Splunk/latest/Data/FormateventsforHTTPEventCollector#Event_metadata
     */
    @ConfigItem
    public Map<String, String> metadataFields = new HashMap<>();

    /**
     * The name of the key used to convey the severity / log level in the metadata fields.
     * Only applicable to 'flat' serialization.
     * With 'nested' serialization, there is already a 'severity' field.
     */
    @ConfigItem(defaultValue = "severity")
    public String metadataSeverityFieldName = "severity";

    /**
     * Determines whether the events are sent in raw mode. In case the raw event (i.e. the actual log message)
     * is not a JSON object you need to explicitly set a source type or Splunk will reject the event (the
     * default source type, _json, assumes that the incoming event can be parsed as JSON)
     *
     * @deprecated Use {@link #serialization}
     */
    @Deprecated(forRemoval = true)
    @ConfigItem(defaultValue = "false")
    public boolean raw = false;

    /**
     * The format of the payload.
     * <ul>
     * <li>With raw serialization, the log message is sent 'as is' in the HTTP body. Metadata can only be common to a whole
     * batch and are sent via HTTP parameters.
     * <li>With nested serialization, the log message is sent into a 'message' field of a JSON structure which also contains
     * dynamic metadata.
     * <li>With flat serialization, the log message is sent into the root 'event' field. Dynamic metadata is sent via the
     * 'fields' root object.
     * </ul>
     */
    @ConfigItem(defaultValue = "nested")
    public SerializationFormat serialization = SerializationFormat.NESTED;

    /**
     * AsyncHandler config
     * <p>
     * This is independent of the SendMode, i.e. whether the HTTP client is async or not.
     */
    @ConfigItem
    AsyncConfig async;

    /**
     * Mirrors com.splunk.logging.HttpEventCollectorSender.SendMode
     */
    public enum SendMode {
        SEQUENTIAL,
        PARALLEL
    }

    public enum SerializationFormat {
        RAW,
        NESTED,
        FLAT
    }

}
