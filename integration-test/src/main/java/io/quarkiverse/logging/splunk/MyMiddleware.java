package io.quarkiverse.logging.splunk;

import java.util.List;

import com.splunk.logging.HttpEventCollectorEventInfo;
import com.splunk.logging.HttpEventCollectorMiddleware;

public class MyMiddleware extends HttpEventCollectorMiddleware.HttpSenderMiddleware {
    @Override
    public void postEvents(
            List<HttpEventCollectorEventInfo> events,
            HttpEventCollectorMiddleware.IHttpSender sender,
            HttpEventCollectorMiddleware.IHttpSenderCallback callback) {
        for (HttpEventCollectorEventInfo event : events) {
            event.getProperties().put("myProperty", "myValue");
        }
        sender.postEvents(events, callback);
    }
}
