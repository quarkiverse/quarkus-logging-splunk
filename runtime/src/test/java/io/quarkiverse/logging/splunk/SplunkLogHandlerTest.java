/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.logging.Formatter;
import java.util.logging.Level;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.splunk.logging.HttpEventCollectorResendMiddleware;
import com.splunk.logging.HttpEventCollectorSender;

@ExtendWith(MockitoExtension.class)
class SplunkLogHandlerTest {

    @Mock
    HttpEventCollectorSender sender;

    @Spy
    Formatter formatter = new PatternFormatter("%s");

    @Test
    void handlerShouldSetSenderOptions() {
        SplunkLogHandler handler = new SplunkLogHandler(sender, true, true, true, true, 1);
        verify(sender).disableCertificateValidation();
        verify(sender).addMiddleware(isA(HttpEventCollectorResendMiddleware.class));
    }

    @Test
    void handlerShouldFormatMessages() {
        SplunkLogHandler handler = new SplunkLogHandler(sender, true, true, true, true, 1);
        handler.setFormatter(formatter);
        ExtLogRecord record = new ExtLogRecord(Level.ALL, "Hello {0}", SplunkLogHandlerTest.class.getName());
        record.setParameters(new String[] { "world" });

        handler.publish(record);

        verify(formatter).format(eq(record));
        verify(sender).send(eq(record.getLevel().toString()),
                eq("Hello world"),
                eq(record.getLoggerName()),
                eq("1"),
                anyMap(),
                isNull(),
                isNull());
    }

    @Test
    void shouldSendRecordUsingHec() {
        SplunkLogHandler handler = new SplunkLogHandler(sender, true, true, true, true, 1);
        handler.setFormatter(formatter);
        ExtLogRecord record = new ExtLogRecord(Level.ALL, "Log Message", SplunkLogHandlerTest.class.getName());
        record.setLoggerName("Logger");
        record.setThreadID(1);
        record.setThrown(new RuntimeException("Exception occurred"));

        handler.publish(record);

        verify(sender).send(eq(record.getLevel().toString()),
                eq(record.getMessage()),
                eq(record.getLoggerName()),
                eq("1"),
                anyMap(),
                eq("Exception occurred"),
                isNull());
    }

    @Test
    void handlerShouldFlushHec() {
        SplunkLogHandler handler = new SplunkLogHandler(sender, true, false, false, false, 0);
        handler.flush();
        verify(sender).flush();
        verifyNoMoreInteractions(sender);
    }

    @Test
    void handlerShouldCloseHecProperly() {
        SplunkLogHandler handler = new SplunkLogHandler(sender, true, false, false, false, 0);
        handler.close();
        verify(sender).flush(eq(true));
        verify(sender).cancel();
        verifyNoMoreInteractions(sender);
    }
}
