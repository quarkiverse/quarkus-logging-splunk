package io.quarkiverse.logging.splunk;

import org.jboss.logmanager.handlers.AsyncHandler;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Copy of io.quarkus.runtime.logging, as the fields are package-private.
 */
@ConfigGroup
public class AsyncConfig {

    /**
     * Indicates whether to log asynchronously
     */
    @ConfigItem(name = ConfigItem.PARENT)
    boolean enable;

    /**
     * The queue length to use before flushing writing
     */
    @ConfigItem(defaultValue = "512")
    int queueLength;

    /**
     * Determine whether to block the publisher (rather than drop the message) when the queue is full
     */
    @ConfigItem(defaultValue = "block")
    AsyncHandler.OverflowAction overflow;
}
