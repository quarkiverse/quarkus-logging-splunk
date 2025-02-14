/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import java.util.List;
import java.util.Locale;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;

import io.quarkiverse.logging.splunk.middleware.HttpEventCollectorResendMiddleware;
import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.filters.AllFilter;


import com.splunk.logging.HttpEventCollectorSender;

public class SplunkLogHandler extends ExtHandler {

    private final HttpEventCollectorSender sender;

    private final boolean includeException;

    private final boolean includeLoggerName;

    private final boolean includeThreadName;

    public SplunkLogHandler(HttpEventCollectorSender sender, boolean includeException, boolean includeLoggerName,
            boolean includeThreadName, boolean disableCertificateValidation, long retriesOnError) {
        this.sender = sender;
        this.includeException = includeException;
        this.includeLoggerName = includeLoggerName;
        this.includeThreadName = includeThreadName;

        if (disableCertificateValidation) {
            this.sender.disableCertificateValidation();
        }
        if (retriesOnError > 0) {
            this.sender.addMiddleware(new HttpEventCollectorResendMiddleware(retriesOnError));
        }
    }

    @Override
    public void doPublish(ExtLogRecord record) {
        String formatted = formatMessage(record);
        if (formatted.isEmpty()) {
            // nothing to write; don't bother
            return;
        }
        this.sender.send(
                record.getMillis(),
                record.getLevel().toString(),
                formatted,
                includeLoggerName ? record.getLoggerName() : null,
                includeThreadName ? String.format(Locale.US, "%d", record.getLongThreadID()) : null,
                record.getMdcCopy(),
                (!includeException || record.getThrown() == null) ? null : record.getThrown().getMessage(),
                null);
    }

    @Override
    public void flush() {
        this.sender.flush();
    }

    @Override
    public void close() throws SecurityException {
        this.sender.flush(true);
        this.sender.cancel();
    }

    private String formatMessage(ExtLogRecord record) {
        String formatted = "";
        final Formatter formatter = getFormatter();
        try {
            formatted = formatter.format(record);
        } catch (Exception ex) {
            reportError("Formatting error", ex, ErrorManager.FORMAT_FAILURE);
        }
        return formatted;
    }

    @Override
    public void setFilter(Filter newFilter) throws SecurityException {
        if (this.getFilter() != null) {
            // setFilter gets called by io.quarkus.runtime.logging.LoggingSetupRecorder with cleanupFilter
            super.setFilter(new AllFilter(List.of(this.getFilter(), newFilter)));
        } else {
            super.setFilter(newFilter);
        }
    }
}
