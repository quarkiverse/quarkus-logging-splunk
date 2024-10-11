package io.quarkiverse.logging.splunk;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;

import com.splunk.logging.HttpEventCollectorSender;

class SplunkLogHandlerRecorderTest {

    @Test
    void shouldSetupSenderWithConfiguredMiddleware() {
        // Arrange
        SplunkHandlerConfig config = createConfig();
        when(config.middleware()).thenReturn(Optional.of(TestMiddleware.class.getName()));

        // Act
        HttpEventCollectorSender sender = SplunkLogHandlerRecorder.createSender(config);
        sender.send("A message");
        sender.flush();

        // Assert
        assertNotNull(TestMiddleware.recordedEvents);
        assertEquals(1, TestMiddleware.recordedEvents.size());
        assertEquals("A message", TestMiddleware.recordedEvents.get(0).getMessage());
    }

    @Test
    void shouldThrowIfMiddlewareIsNotInstantiable() {
        // Arrange
        SplunkHandlerConfig config = createConfig();
        when(config.middleware()).thenReturn(Optional.of("NonExistentMiddleware"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> SplunkLogHandlerRecorder.createSender(config));
    }

    @Test
    void shouldThrowIfMiddlewareIsNotOfCorrectType() {
        // Arrange
        SplunkHandlerConfig config = createConfig();
        when(config.middleware()).thenReturn(Optional.of(Object.class.getName()));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> SplunkLogHandlerRecorder.createSender(config));
    }

    private SplunkHandlerConfig createConfig() {
        SplunkHandlerConfig config = mock(SplunkHandlerConfig.class);
        when(config.token()).thenReturn(Optional.of("token"));
        when(config.channel()).thenReturn(Optional.empty());
        when(config.metadataIndex()).thenReturn(Optional.empty());
        when(config.metadataHost()).thenReturn(Optional.empty());
        when(config.metadataSource()).thenReturn(Optional.empty());
        when(config.metadataSourceType()).thenReturn(Optional.empty());

        // override with default values
        when(config.enabled()).thenReturn(true);
        when(config.level()).thenReturn(Level.ALL);
        when(config.url()).thenReturn("https://localhost:8088/");
        when(config.disableCertificateValidation()).thenReturn(false);
        when(config.sendMode()).thenReturn(SplunkHandlerConfig.SendMode.SEQUENTIAL);
        when(config.batchInterval()).thenReturn(Duration.ofSeconds(10));
        when(config.batchSizeCount()).thenReturn(10L);
        when(config.batchSizeBytes()).thenReturn(10240L);
        when(config.maxRetries()).thenReturn(0L);
        when(config.format()).thenReturn("%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n");
        when(config.includeException()).thenReturn(false);
        when(config.includeLoggerName()).thenReturn(false);
        when(config.includeThreadName()).thenReturn(false);
        when(config.metadataSeverityFieldName()).thenReturn("severity");
        when(config.serialization()).thenReturn(SplunkHandlerConfig.SerializationFormat.NESTED);
        when(config.connectTimeout()).thenReturn(3000L);
        when(config.callTimeout()).thenReturn(0L);
        when(config.readTimeout()).thenReturn(10000L);
        when(config.writeTimeout()).thenReturn(10000L);
        when(config.terminationTimeout()).thenReturn(0L);

        return config;
    }

}
