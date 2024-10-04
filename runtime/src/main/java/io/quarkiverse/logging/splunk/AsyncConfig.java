package io.quarkiverse.logging.splunk;

import org.jboss.logmanager.handlers.AsyncHandler;

import io.smallrye.config.WithDefault;
import io.smallrye.config.WithParentName;

/**
 * Copy of io.quarkus.runtime.logging, as the fields are package-private.
 */
public interface AsyncConfig {

    /**
     * Indicates whether to log asynchronously
     */
    @WithDefault("false")
    @WithParentName
    boolean enable();

    /**
     * The queue length to use before flushing writing
     */
    @WithDefault("512")
    int queueLength();

    /**
     * Determine whether to block the publisher (rather than drop the message) when the queue is full
     */
    @WithDefault("block")
    AsyncHandler.OverflowAction overflow();
}
