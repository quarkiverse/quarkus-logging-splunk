/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import static io.quarkiverse.logging.splunk.SplunkHandlerConfig.ExceptionEncoding.JSON;
import static io.quarkiverse.logging.splunk.SplunkHandlerConfig.ExceptionEncoding.MESSAGE_ONLY;
import static java.util.Map.entry;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.Serializable;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.internal.matchers.ContainsExtraTypeInfo;
import org.mockito.internal.matchers.text.ValuePrinter;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
        SplunkLogHandler handler = new SplunkLogHandler(sender, true, MESSAGE_ONLY, true, true, true, 1);
        verify(sender).disableCertificateValidation();
        verify(sender).addMiddleware(isA(HttpEventCollectorResendMiddleware.class));
    }

    @Test
    void handlerShouldFormatMessages() {
        SplunkLogHandler handler = new SplunkLogHandler(sender, true, MESSAGE_ONLY, true, true, true, 1);
        handler.setFormatter(formatter);
        ExtLogRecord record = new ExtLogRecord(Level.ALL, "Hello {0}", SplunkLogHandlerTest.class.getName());
        record.setParameters(new String[] { "world" });

        handler.publish(record);

        verify(formatter).format(eq(record));
        verify(sender).send(anyLong(),
                eq(record.getLevel().toString()),
                eq("Hello world"),
                eq(record.getLoggerName()),
                eq("1"),
                anyMap(),
                isNull(),
                isNull());
    }

    @Test
    void shouldSendRecordUsingHec() {
        SplunkLogHandler handler = new SplunkLogHandler(sender, true, MESSAGE_ONLY, true, true, true, 1);
        handler.setFormatter(formatter);
        ExtLogRecord record = new ExtLogRecord(Level.ALL, "Log Message", SplunkLogHandlerTest.class.getName());
        record.setLoggerName("Logger");
        record.setThreadID(1);
        record.setThrown(new RuntimeException("Exception occurred"));

        handler.publish(record);

        verify(sender).send(anyLong(),
                eq(record.getLevel().toString()),
                eq(record.getMessage()),
                eq(record.getLoggerName()),
                eq("1"),
                anyMap(),
                eq("Exception occurred"),
                isNull());
    }

    @Test
    void shouldEncodeExceptionsAsJsonWhenConfigured() throws Exception {
        SplunkLogHandler handler = new SplunkLogHandler(sender, true, JSON, true, true, true, 1);
        handler.setFormatter(formatter);
        ExtLogRecord record = new ExtLogRecord(Level.ALL, "Log Message", SplunkLogHandlerTest.class.getName());
        record.setLoggerName("Logger");
        record.setThreadID(1);
        record.setThrown(new RuntimeException("Exception occurred"));

        handler.publish(record);

        verify(sender).send(anyLong(),
                eq(record.getLevel().toString()),
                eq(record.getMessage()),
                eq(record.getLoggerName()),
                eq("1"),
                anyMap(),
                argThat(new ExceptionJsonMatcher(record.getThrown())),
                isNull());
    }

    @Test
    void handlerShouldFlushHec() {
        SplunkLogHandler handler = new SplunkLogHandler(sender, true, MESSAGE_ONLY, false, false, false, 0);
        handler.flush();
        verify(sender).flush();
        verifyNoMoreInteractions(sender);
    }

    @Test
    void handlerShouldCloseHecProperly() {
        SplunkLogHandler handler = new SplunkLogHandler(sender, true, MESSAGE_ONLY, false, false, false, 0);
        handler.close();
        verify(sender).flush(eq(true));
        verify(sender).cancel();
        verifyNoMoreInteractions(sender);
    }

    private static class ExceptionJsonMatcher implements ArgumentMatcher<String>, ContainsExtraTypeInfo {
        private Map<String, ? extends Serializable> wanted;

        public ExceptionJsonMatcher(Throwable throwable) {
            wanted = Map.ofEntries(
                    Map.entry("detailMessage", throwable.getMessage()),
                    Map.entry("exceptionClass", throwable.getClass().toString()),
                    Map.entry("fileName", throwable.getStackTrace()[0].getFileName()),
                    Map.entry("methodName", throwable.getStackTrace()[0].getMethodName()),
                    Map.entry("lineNumber", String.valueOf(throwable.getStackTrace()[0].getLineNumber())));
        }

        @Override
        public String toString() {
            return ValuePrinter.print(wanted);
        }

        @Override
        public boolean matches(String json) {
            Map<String, String> exceptionFields = new Gson().fromJson(json, new TypeToken<>() {
            });

            return wanted.equals(exceptionFields);
        }

        @Override
        public String toStringWithType(String className) {
            return "";
        }

        @Override
        public boolean typeMatches(Object target) {
            return false;
        }

        @Override
        public Object getWanted() {
            return wanted;
        }
    }
}
