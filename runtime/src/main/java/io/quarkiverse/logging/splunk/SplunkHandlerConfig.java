/*
Copyright (c) 2023 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio, Yohann Puyhaubert (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import com.splunk.logging.HttpEventCollectorSender;

import io.smallrye.config.WithDefault;

/**
 * The configuration of the Splunk root or any Splunk named handler.
 */
public interface SplunkHandlerConfig {
    /**
     * Determine whether to enable the handler
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The splunk handler log level. By default, it is no more strict than the root handler level.
     */
    @WithDefault("ALL")
    Level level();

    /**
     * Splunk HEC endpoint base url.
     * <p>
     * With raw events, the endpoint targeted is /services/collector/raw.
     * With flat or nested JSON events, the endpoint targeted is /services/collector/event/1.0.
     */
    @WithDefault("https://localhost:8088/")
    String url();

    /**
     * Disable TLS certificate validation with HEC endpoint
     */
    @WithDefault("false")
    boolean disableCertificateValidation();

    /**
     * The application token to authenticate with HEC, the token is mandatory if the extension is enabled
     * https://docs.splunk.com/Documentation/Splunk/latest/Data/FormateventsforHTTPEventCollector#HEC_token
     */
    Optional<String> token();

    /**
     * The strategy to send events to HEC.
     * <p>
     * In sequential mode, there is only one HTTP connection to HEC and the order of events is preserved, but performance is
     * lower.
     * In parallel mode, event batches are sent asynchronously over multiple HTTP connections, and events with the same
     * timestamp
     * (that has 1 millisecond resolution) may be indexed out of order by Splunk.
     */
    @WithDefault("sequential")
    SendMode sendMode();

    /**
     * A GUID to identify an HEC client and guarantee isolation at HEC level in case of slow clients.
     *
     * @see <a href=
     *      "https://docs.splunk.com/Documentation/Splunk/latest/Data/AboutHECIDXAck#About_channels_and_sending_data">splunk
     *      guide</a>
     */
    Optional<String> channel();

    /**
     * Batching delay before sending a group of events.
     * <p>
     * If 0, the events are sent immediately.
     * </p>
     */
    @WithDefault("10s")
    Duration batchInterval();

    /**
     * Maximum number of events in a batch. By default 10, if 0 no batching.
     */
    @WithDefault("10")
    long batchSizeCount();

    /**
     * Maximum total size in bytes of events in a batch. By default 10KB, if 0 no batching.
     */
    @WithDefault("10240")
    long batchSizeBytes();

    /**
     * Maximum number of retries in case of I/O exceptions with HEC connection.
     */
    @WithDefault("0")
    long maxRetries();

    /**
     * A middleware to customize the behavior of sending events to Splunk.
     *
     * @see com.splunk.logging.HttpEventCollectorMiddleware
     */
    Optional<String> middleware();

    /**
     * Whether to log events that could not be sent to Splunk when a failure occurred (after retries)
     * using the standard output of the process.
     * Applications that deal with sensitive data may want to disable this
     *
     * @see <a href=
     *      "https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html#data-to-exclude">OWASP logging cheat
     *      guide</a>
     */
    @WithDefault("true")
    boolean printEventsToStdoutOnError();

    /**
     * The log format, defining which metadata are inlined inside the log main payload.
     * <p>
     * Specific metadata (hostname, category, thread name, ...), as well as MDC key/value map, can also be sent in a structured
     * way.
     */
    @WithDefault("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n")
    String format();

    /**
     * Whether to send the thrown exception message as a structured metadata of the log event (as opposed to %e in a formatted
     * message, it does not include the exception name or stacktrace).
     * Only applicable to 'nested' serialization.
     */
    @WithDefault("false")
    boolean includeException();

    /**
     * Whether to send the logger name as a structured metadata of the log event (equivalent of %c in a formatted message).
     * Only applicable to 'nested' serialization.
     */
    @WithDefault("false")
    boolean includeLoggerName();

    /**
     * Whether to send the thread name as a structured metadata of the log event (equivalent of %t in a formatted message).
     * Only applicable to 'nested' serialization.
     */
    @WithDefault("false")
    boolean includeThreadName();

    /**
     * Overrides the host name metadata value.
     * <p>
     * Default value: the equivalent of %h in a formatted message.
     * </p>
     */
    Optional<String> metadataHost();

    /**
     * The source value to assign to the event data. For example, if you're sending data from an app you're developing,
     * you could set this key to the name of the app.
     *
     * @see <a href=
     *      "https://docs.splunk.com/Documentation/Splunk/latest/Data/FormateventsforHTTPEventCollector#Event_metadata">splunk
     *      guide</a>
     */
    Optional<String> metadataSource();

    /**
     * The optional format of the events, to enable some parsing on Splunk side.
     *
     * <p>
     * A given source type may have indexed fields extraction enabled, which is the case of the built-in _json used for nested
     * serialization.
     * </p>
     * <p>
     * Default value: _json for nested serialization, not set otherwise
     * </p>
     *
     * @see <a href=
     *      "https://docs.splunk.com/Documentation/Splunk/latest/Data/FormateventsforHTTPEventCollector#Event_metadata">splunk
     *      guide</a>
     */
    Optional<String> metadataSourceType();

    /**
     * The optional name of the index by which the event data is to be stored. If set, it must be within the
     * list of allowed indexes of the token (if it has the indexes parameter set).
     *
     * @see <a href=
     *      "https://docs.splunk.com/Documentation/Splunk/latest/Data/FormateventsforHTTPEventCollector#Event_metadata">splunk
     *      guide</a>
     */
    Optional<String> metadataIndex();

    /**
     * Optional static key/value pairs to populate the "fields" key of event metadata. This isn't
     * applicable to raw serialization.
     *
     * @see <a href=
     *      "https://docs.splunk.com/Documentation/Splunk/latest/Data/FormateventsforHTTPEventCollector#Event_metadata">splunk
     *      guide</a>
     */
    Map<String, String> metadataFields();

    /**
     * The name of the key used to convey the severity / log level in the metadata fields.
     * Only applicable to 'flat' serialization.
     * With 'nested' serialization, there is already a 'severity' field.
     */
    @WithDefault("severity")
    String metadataSeverityFieldName();

    /**
     * Determines whether the events are sent in raw mode. In case the raw event (i.e. the actual log message)
     * is not a JSON object you need to explicitly set a source type or Splunk will reject the event (the
     * default source type, _json, assumes that the incoming event can be parsed as JSON)
     *
     * @deprecated Use {@link #serialization}
     */
    @Deprecated(forRemoval = true)
    @WithDefault("false")
    boolean raw();

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
    @WithDefault("nested")
    SerializationFormat serialization();

    /**
     * The name of the named filter to link to the splunk handler.
     */
    Optional<String> filter();

    /**
     * AsyncHandler config
     * <p>
     * This is independent of the SendMode, i.e. whether the HTTP client is async or not.
     */
    AsyncConfig async();

    /**
     * Mirrors com.splunk.logging.HttpEventCollectorSender.SendMode
     */
    enum SendMode {
        SEQUENTIAL,
        PARALLEL
    }

    enum SerializationFormat {
        RAW,
        NESTED,
        FLAT
    }

    /**
     * Sets the default connect timeout for new connections in milliseconds.
     */
    @WithDefault("3000")
    long connectTimeout();

    /**
     * Sets the default timeout for complete calls in milliseconds.
     */
    @WithDefault("0")
    long callTimeout();

    /**
     * Sets the default read timeout for new connections in milliseconds.
     */
    @WithDefault("10000")
    long readTimeout();

    /**
     * Sets the default write timeout for new connections in milliseconds.
     */
    @WithDefault("10000")
    long writeTimeout();

    /**
     * Sets the default termination timeout during a flush in milliseconds.
     */
    @WithDefault("0")
    long terminationTimeout();
}
