package io.quarkiverse.logging.splunk;

import java.util.HashMap;
import java.util.Map;

import com.splunk.logging.EventBodySerializer;
import com.splunk.logging.EventHeaderSerializer;
import com.splunk.logging.HttpEventCollectorEventInfo;

class SplunkFlatEventSerializer implements EventHeaderSerializer, EventBodySerializer {

    private final String metadataSeverityFieldName;

    SplunkFlatEventSerializer(String metadataSeverityFieldName) {
        this.metadataSeverityFieldName = metadataSeverityFieldName;
    }

    ;

    /**
     * Serialization of the root JSON object of the event
     */
    @Override
    public Map<String, Object> serializeEventHeader(HttpEventCollectorEventInfo eventInfo, Map<String, Object> metadata) {
        Map<String, String> fields = (Map<String, String>) metadata.computeIfAbsent("fields", k -> new HashMap<>());
        fields.put(this.metadataSeverityFieldName, eventInfo.getSeverity());
        if (eventInfo.getLoggerName() != null) {
            fields.put("logger", eventInfo.getLoggerName());
        }
        if (eventInfo.getThreadName() != null) {
            fields.put("thread", eventInfo.getThreadName());
        }
        if (eventInfo.getExceptionMessage() != null) {
            fields.put("exception", eventInfo.getExceptionMessage());
        }
        fields.putAll(eventInfo.getProperties());
        return metadata;
    }

    /**
     * Serialization of the "event" field
     */
    @Override
    public String serializeEventBody(HttpEventCollectorEventInfo eventInfo, Object formattedMessage) {
        return eventInfo.getMessage();
    }

    /**
     * We have to override this, because, by default, splunk-library-java does not send timestamp to Splunk.
     * Refer to <a href="https://github.com/splunk/splunk-library-javalogging/pull/165">this pull-request on github</a>
     */
    @Override
    public double getEventTime(HttpEventCollectorEventInfo eventInfo) {
        return eventInfo.getTime();
    }

}
