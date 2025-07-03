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
    boolean enabled();

    /**
     * Indicates whether to log asynchronously
     *
     * @deprecated Use {@link #enabled()} instead. This method is kept for backward compatibility. Deprecated because it
     *             breaks YAML and JSON parsing, when simultaneously has boolean value and sub-nodes like queueLength and
     *             overflow.
     */
    @WithDefault("false")
    @WithParentName
    @Deprecated
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
