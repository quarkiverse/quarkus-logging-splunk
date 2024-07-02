package io.quarkiverse.logging.splunk;

import java.util.List;

import com.splunk.logging.HttpEventCollectorEventInfo;
import com.splunk.logging.HttpEventCollectorMiddleware;

/**
 * Middleware for testing purposes. It only records the events.
 */
public class TestMiddleware extends HttpEventCollectorMiddleware.HttpSenderMiddleware {

    static List<HttpEventCollectorEventInfo> recordedEvents;

    @Override
    public void postEvents(
            List<HttpEventCollectorEventInfo> events,
            HttpEventCollectorMiddleware.IHttpSender sender,
            HttpEventCollectorMiddleware.IHttpSenderCallback callback) {
        recordedEvents = events;
    }
}
