/*
Copyright (c) 2023 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio, Yohann Puyhaubert (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.AsyncHandler;

import com.splunk.logging.HttpEventCollectorErrorHandler;
import com.splunk.logging.HttpEventCollectorSender;
import com.splunk.logging.hec.MetadataTags;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class SplunkLogHandlerRecorder {

    public RuntimeValue<Optional<Handler>> initializeHandler(SplunkConfig rootConfig) {
        if (!rootConfig.config.enabled) {
            return new RuntimeValue<>(Optional.empty());
        }

        Handler handler = buildHandlerFromConfig(rootConfig.config);
        return new RuntimeValue<>(Optional.of(handler));
    }

    public RuntimeValue<Map<String, Handler>> initializeHandlers(SplunkConfig rootConfig) {
        if (rootConfig.namedHandlers == null || rootConfig.namedHandlers.isEmpty()) {
            return new RuntimeValue<>(Collections.EMPTY_MAP);
        }

        Map<String, Handler> namedHandlers = rootConfig.namedHandlers
                .entrySet()
                .stream()
                .filter(e -> e.getValue().enabled)
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> buildHandlerFromConfig(e.getValue())));

        return new RuntimeValue<>(namedHandlers);
    }

    private Handler buildHandlerFromConfig(SplunkHandlerConfig config) {
        if (!config.token.isPresent()) {
            throw new IllegalArgumentException("The property quarkus.log.handler.splunk.token is mandatory");
        }
        HttpEventCollectorSender sender = createSender(config);
        SplunkLogHandler splunkLogHandler = createSplunkLogHandler(sender, config);
        splunkLogHandler.setLevel(config.level);
        splunkLogHandler.setFormatter(
                new PatternFormatter(config.format));

        Handler handler = config.async.enable
                ? createAsyncHandler(config.async,
                        config.level, splunkLogHandler)
                : splunkLogHandler;
        return handler;
    }

    static HttpEventCollectorSender createSender(SplunkHandlerConfig config) {
        HttpEventCollectorErrorHandler.onError(new SplunkErrorCallback());
        String type = "";
        if (config.raw || config.serialization == SplunkHandlerConfig.SerializationFormat.RAW) {
            type = "Raw";
        }
        // Timeout settings is not used and passing a null is correct regarding the code
        HttpEventCollectorSender sender = new HttpEventCollectorSender(
                config.url,
                config.token.get(),
                config.channel.orElse(""),
                type,
                config.batchInterval.toMillis(),
                config.batchSizeCount,
                config.batchSizeBytes,
                config.sendMode.name().toLowerCase(),
                buildMetadata(config), null);
        if (config.serialization == SplunkHandlerConfig.SerializationFormat.FLAT) {
            SplunkFlatEventSerializer serializer = new SplunkFlatEventSerializer(config.metadataSeverityFieldName);
            sender.setEventHeaderSerializer(serializer);
            sender.setEventBodySerializer(serializer);
        }
        return sender;
    }

    static Map<String, String> buildMetadata(SplunkHandlerConfig config) {
        HashMap<String, String> metadata = new HashMap<>();
        // Note: sending an empty index is invalid, the index property has to be omitted
        config.metadataIndex.ifPresent(s -> metadata.put(MetadataTags.INDEX, s));
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            metadata.put(MetadataTags.HOST, config.metadataHost.orElse(hostName));
        } catch (UnknownHostException e) {
            // Ignore
        }
        config.metadataSource.ifPresent(s -> metadata.put(MetadataTags.SOURCE, s));

        if (config.metadataSourceType.isPresent()) {
            metadata.put(MetadataTags.SOURCETYPE, config.metadataSourceType.get());
        } else if (config.serialization == SplunkHandlerConfig.SerializationFormat.NESTED) {
            metadata.put(MetadataTags.SOURCETYPE, "_json");
        }

        metadata.putAll(config.metadataFields);
        return metadata;
    }

    private SplunkLogHandler createSplunkLogHandler(HttpEventCollectorSender sender,
            SplunkHandlerConfig config) {
        return new SplunkLogHandler(sender,
                config.includeException,
                config.includeLoggerName,
                config.includeThreadName,
                config.disableCertificateValidation,
                config.maxRetries);
    }

    private static AsyncHandler createAsyncHandler(AsyncConfig asyncConfig, Level level, Handler handler) {
        final AsyncHandler asyncHandler = new AsyncHandler(asyncConfig.queueLength);
        asyncHandler.setOverflowAction(asyncConfig.overflow);
        asyncHandler.addHandler(handler);
        asyncHandler.setLevel(level);
        return asyncHandler;
    }
}
