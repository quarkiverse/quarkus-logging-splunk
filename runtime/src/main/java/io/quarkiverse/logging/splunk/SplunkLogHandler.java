/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.ErrorManager;
import java.util.logging.Filter;
import java.util.logging.Formatter;

import jakarta.annotation.Nonnull;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.filters.AllFilter;

import com.google.gson.Gson;
import com.splunk.logging.HttpEventCollectorResendMiddleware;
import com.splunk.logging.HttpEventCollectorSender;

public class SplunkLogHandler extends ExtHandler {

    private final HttpEventCollectorSender sender;

    private final boolean includeException;

    private final SplunkHandlerConfig.ExceptionEncoding exceptionEncoding;

    private final boolean includeLoggerName;

    private final boolean includeThreadName;

    public SplunkLogHandler(HttpEventCollectorSender sender, boolean includeException,
            SplunkHandlerConfig.ExceptionEncoding exceptionEncoding, boolean includeLoggerName,
            boolean includeThreadName, boolean disableCertificateValidation, long retriesOnError) {
        this.sender = sender;
        this.includeException = includeException;
        this.includeLoggerName = includeLoggerName;
        this.includeThreadName = includeThreadName;
        this.exceptionEncoding = exceptionEncoding;

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
        if (formatted.length() == 0) {
            // nothing to write; don't bother
            return;
        }
        this.sender.send(
                record.getMillis(),
                record.getLevel().toString(),
                formatted,
                includeLoggerName ? record.getLoggerName() : null,
                includeThreadName ? String.format(Locale.US, "%d", record.getThreadID()) : null,
                record.getMdcCopy(),
                (!includeException || record.getThrown() == null) ? null : encodeException(record.getThrown()),
                null);
    }

    private String encodeException(@Nonnull Throwable thrown) {
        return switch (this.exceptionEncoding) {
            case MESSAGE_ONLY -> thrown.getMessage();
            case JSON -> generateErrorDetail(thrown);
        };
    }

    /**
     * {@inheritDoc}
     * <p>
     * Warning: explicit calls to flush bypass event batching checks, so events are sent too early. Do not rely on APIs
     * calling flush directly, like the AsyncHandler's autoflush mechanism.
     */
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

    /**
     * Method used to generate proper exception message if any exception encountered.
     *
     * @param throwable the exception in the event
     * @return the processed string of all exception detail
     */
    private String generateErrorDetail(final Throwable throwable) {
        String exceptionDetail = "";

        /*
         * Exception details are only populated when any ERROR OR FATAL event occurred
         */
        try {
            // Exception thrown in application is wrapped with relevant information instead of just a message.
            Map<String, String> exceptionDetailMap = new LinkedHashMap<>();

            exceptionDetailMap.put("detailMessage", throwable.getMessage());
            exceptionDetailMap.put("exceptionClass", throwable.getClass().toString());

            StackTraceElement[] elements = throwable.getStackTrace();
            // Retrieving first element from elements array is because the throws exception detail would be available as a first element.
            if (elements != null && elements.length > 0 && elements[0] != null) {
                exceptionDetailMap.put("fileName", elements[0].getFileName());
                exceptionDetailMap.put("methodName", elements[0].getMethodName());
                exceptionDetailMap.put("lineNumber", String.valueOf(elements[0].getLineNumber()));
            }
            exceptionDetail = new Gson().toJson(exceptionDetailMap);
        } catch (Exception e) {
            // No action here
        }

        return exceptionDetail;
    }
}
