/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Handler;

import org.jboss.logmanager.formatters.PatternFormatter;

import com.splunk.logging.HttpEventCollectorErrorHandler;
import com.splunk.logging.HttpEventCollectorSender;
import com.splunk.logging.hec.MetadataTags;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class SplunkLogHandlerRecorder {

    public RuntimeValue<Optional<Handler>> initializeHandler(SplunkConfig config) {
        if (!config.enabled) {
            return new RuntimeValue<>(Optional.empty());
        }
        if (!config.token.isPresent()) {
            throw new IllegalArgumentException("The property quarkus.log.handler.splunk.token is mandatory");
        }
        HttpEventCollectorSender sender = createSender(config);
        SplunkLogHandler splunkLogHandler = createSplunkLogHandler(sender, config);
        splunkLogHandler.setLevel(config.level);
        splunkLogHandler.setFormatter(new PatternFormatter(config.format));
        return new RuntimeValue<>(Optional.of(splunkLogHandler));
    }

    static HttpEventCollectorSender createSender(SplunkConfig config) {
        HttpEventCollectorErrorHandler.onError(new SplunkErrorCallback());
        String type = config.raw ? "Raw" : "";
        // Timeout settings is not used and passing a null is correct regarding the code
        return new HttpEventCollectorSender(
                config.url, config.token.get(), config.channel.orElse(""), type,
                config.batchInterval.getSeconds(),
                config.batchSizeCount, config.batchSizeBytes,
                config.sendMode.name().toLowerCase(), buildMetadata(config), null);
    }

    static Map<String, String> buildMetadata(SplunkConfig config) {
        HashMap<String, String> metadata = new HashMap<>();
        // Note: sending an empty index is invalid, the index property has to be omitted
        config.metadataIndex.ifPresent(s -> metadata.put(MetadataTags.INDEX, s));
        //metadata.put(MetadataTags.INDEX, config.metadataIndex.orElse(""));
        try {
            String hostName = InetAddress.getLocalHost().getHostName();
            metadata.put(MetadataTags.HOST, config.metadataHost.orElse(hostName));
        } catch (UnknownHostException e) {
            // Ignore
        }
        config.metadataSource.ifPresent(s -> metadata.put(MetadataTags.SOURCE, s));
        metadata.put(MetadataTags.SOURCETYPE, config.metadataSourceType);

        for (Entry<String, String> entry : config.metadataFields.entrySet()) {
            metadata.put(entry.getKey(), entry.getValue());
        }

        return metadata;
    }

    private SplunkLogHandler createSplunkLogHandler(HttpEventCollectorSender sender, SplunkConfig config) {
        return new SplunkLogHandler(sender, config.includeException, config.includeLoggerName, config.includeThreadName,
                config.disableCertificateValidation,
                config.maxRetries);
    }
}
