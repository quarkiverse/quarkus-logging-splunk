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
import java.util.function.BiConsumer;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.jboss.logmanager.formatters.PatternFormatter;
import org.jboss.logmanager.handlers.AsyncHandler;

import com.splunk.logging.HttpEventCollectorErrorHandler;
import com.splunk.logging.HttpEventCollectorMiddleware.HttpSenderMiddleware;
import com.splunk.logging.HttpEventCollectorSender;
import com.splunk.logging.hec.MetadataTags;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.logging.DiscoveredLogComponents;
import io.quarkus.runtime.logging.LogFilterFactory;

@Recorder
public class SplunkLogHandlerRecorder {

    public RuntimeValue<Optional<Handler>> initializeHandler(SplunkConfig rootConfig,
            DiscoveredLogComponents discoveredLogComponents) {
        if (!rootConfig.config.enabled) {
            return new RuntimeValue<>(Optional.empty());
        }

        Handler handler = buildHandlerFromConfig(rootConfig.config, discoveredLogComponents);
        return new RuntimeValue<>(Optional.of(handler));
    }

    public RuntimeValue<Map<String, Handler>> initializeHandlers(SplunkConfig rootConfig,
            DiscoveredLogComponents discoveredLogComponents) {
        if (rootConfig.namedHandlers == null || rootConfig.namedHandlers.isEmpty()) {
            return new RuntimeValue<>(Collections.EMPTY_MAP);
        }

        Map<String, Handler> namedHandlers = rootConfig.namedHandlers
                .entrySet()
                .stream()
                .filter(e -> e.getValue().enabled)
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> buildHandlerFromConfig(e.getValue(), discoveredLogComponents)));

        return new RuntimeValue<>(namedHandlers);
    }

    private Handler buildHandlerFromConfig(SplunkHandlerConfig config, DiscoveredLogComponents discoveredLogComponents) {
        if (!config.token.isPresent()) {
            throw new IllegalArgumentException("The property quarkus.log.handler.splunk.token is mandatory");
        }
        HttpEventCollectorSender sender = createSender(config);
        SplunkLogHandler splunkLogHandler = createSplunkLogHandler(sender, config);
        splunkLogHandler.setLevel(config.level);
        splunkLogHandler.setFormatter(
                new PatternFormatter(config.format));
        applyFilter(discoveredLogComponents, config.filter, splunkLogHandler);

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

        HttpEventCollectorSender.TimeoutSettings rto = null;
        if ((config.connectTimeout != HttpEventCollectorSender.TimeoutSettings.DEFAULT_CONNECT_TIMEOUT)
                || (config.callTimeout != HttpEventCollectorSender.TimeoutSettings.DEFAULT_CALL_TIMEOUT) ||
                (config.readTimeout != HttpEventCollectorSender.TimeoutSettings.DEFAULT_READ_TIMEOUT)
                || (config.writeTimeout != HttpEventCollectorSender.TimeoutSettings.DEFAULT_WRITE_TIMEOUT) ||
                (config.terminationTimeout != HttpEventCollectorSender.TimeoutSettings.DEFAULT_TERMINATION_TIMEOUT)) {
            rto = new HttpEventCollectorSender.TimeoutSettings(config.connectTimeout, config.callTimeout,
                    config.readTimeout, config.writeTimeout, config.terminationTimeout);
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
                buildMetadata(config), rto);
        if (config.serialization == SplunkHandlerConfig.SerializationFormat.FLAT) {
            SplunkFlatEventSerializer serializer = new SplunkFlatEventSerializer(config.metadataSeverityFieldName);
            sender.setEventHeaderSerializer(serializer);
            sender.setEventBodySerializer(serializer);
        }
        if (config.middleware.isPresent()) {
            try {
                sender.addMiddleware(
                        Thread.currentThread().getContextClassLoader().loadClass(config.middleware.get())
                                .asSubclass(HttpSenderMiddleware.class)
                                .getDeclaredConstructor()
                                .newInstance());
            } catch (Exception e) {
                throw new IllegalArgumentException("Could not instantiate middleware " + config.middleware.get(), e);
            }
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

    private static void applyFilter(DiscoveredLogComponents discoveredLogComponents,
            Optional<String> filterName, Handler handler) {
        if (filterName.isPresent()) {
            Map<String, Filter> namedFilters = createNamedFilters(discoveredLogComponents);
            String name = filterName.get();
            Filter namedFilter = namedFilters.get(name);
            if (namedFilter == null) {
                throw new IllegalStateException("Unable to find named filter '" + name);
            } else {
                handler.setFilter(namedFilter);
            }
        }
    }

    private static Map<String, Filter> createNamedFilters(DiscoveredLogComponents discoveredLogComponents) {
        if (discoveredLogComponents.getNameToFilterClass().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Filter> nameToFilter = new HashMap<>();
        LogFilterFactory logFilterFactory = LogFilterFactory.load();
        discoveredLogComponents.getNameToFilterClass().forEach(new BiConsumer<>() {
            @Override
            public void accept(String name, String className) {
                try {
                    nameToFilter.put(name, logFilterFactory.create(className));
                } catch (Exception e) {
                    throw new RuntimeException("Unable to create instance of Logging Filter '" + className + "'");
                }
            }
        });
        return nameToFilter;
    }
}
