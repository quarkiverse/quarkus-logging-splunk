package io.quarkiverse.logging.splunk;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

import io.quarkus.logging.LoggingFilter;

@LoggingFilter(name = "sensitive-filter")
public class SensitiveLogFilter implements Filter {

    @Override
    public boolean isLoggable(LogRecord record) {
        if (record.getMessage().contains("Sensitive")) {
            record.setMessage(record.getMessage().replace("Sensitive", "*********"));
        }
        return true;
    }
}
