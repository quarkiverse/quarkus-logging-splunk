package io.quarkiverse.logging.splunk;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.splunk.logging.HttpEventCollectorSender;

class SplunkLogHandlerRecorderTest {

    @Test
    void shouldSetupSenderWithConfiguredMiddleware() {
        // Arrange
        SplunkHandlerConfig config = createConfig();
        config.middleware = Optional.of(TestMiddleware.class.getName());

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
        config.middleware = Optional.of("NonExistentMiddleware");

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> SplunkLogHandlerRecorder.createSender(config));
    }

    @Test
    void shouldThrowIfMiddlewareIsNotOfCorrectType() {
        // Arrange
        SplunkHandlerConfig config = createConfig();
        config.middleware = Optional.of(Object.class.getName());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> SplunkLogHandlerRecorder.createSender(config));
    }

    private SplunkHandlerConfig createConfig() {
        SplunkHandlerConfig config = new SplunkHandlerConfig();
        config.token = Optional.of("token");
        config.channel = Optional.empty();
        config.metadataIndex = Optional.empty();
        config.metadataHost = Optional.empty();
        config.metadataSource = Optional.empty();
        config.metadataSourceType = Optional.empty();
        return config;
    }
}
