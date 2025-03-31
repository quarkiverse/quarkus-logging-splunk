package io.quarkiverse.logging.splunk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Optional;
import java.util.logging.Handler;

import org.jboss.logmanager.handlers.AsyncHandler;
import org.junit.jupiter.api.Test;

import com.splunk.logging.HttpEventCollectorSender;

import io.quarkus.runtime.RuntimeValue;

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

    @Test
    void shouldCreateAsyncHandlerWithoutAutoflush() {
        // Arrange
        SplunkConfig rootConfig = createAsyncRootConfig();

        // Act
        RuntimeValue<Optional<Handler>> handler = new SplunkLogHandlerRecorder().initializeHandler(rootConfig, null);

        // Assert
        assertTrue(handler.getValue().isPresent());
        AsyncHandler asyncHandler = assertInstanceOf(AsyncHandler.class, handler.getValue().get());
        assertFalse(asyncHandler.isAutoFlush());
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

    private SplunkConfig createAsyncRootConfig() {
        SplunkHandlerConfig handlerConfig = createConfig();
        handlerConfig.filter = Optional.empty();
        handlerConfig.middleware = Optional.of(TestMiddleware.class.getName());
        // Override batchInterval duration
        handlerConfig.batchInterval = Duration.ofSeconds(10);

        SplunkConfig rootConfig = new SplunkConfig();
        rootConfig.config = handlerConfig;

        // Enable async
        AsyncConfig asyncConfig = new AsyncConfig();
        asyncConfig.enable = true;
        asyncConfig.queueLength = 512;
        asyncConfig.overflow = AsyncHandler.OverflowAction.BLOCK;
        handlerConfig.async = asyncConfig;
        return rootConfig;
    }

}
