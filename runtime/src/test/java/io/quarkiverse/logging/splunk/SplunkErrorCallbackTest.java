/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.logmanager.handlers.ConsoleHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.splunk.logging.HttpEventCollectorEventInfo;

import io.quarkus.bootstrap.logging.InitialConfigurator;

@ExtendWith(MockitoExtension.class)
public class SplunkErrorCallbackTest {

    ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @Spy
    PrintStream stdout = new PrintStream(outContent);

    @Spy
    PrintStream stderr = new PrintStream(errContent);

    HttpEventCollectorEventInfo logEvent = new HttpEventCollectorEventInfo(System.currentTimeMillis(), "INFO", "Hello",
            SplunkErrorCallbackTest.class.getName(), "thread", null, null, null);

    @AfterEach
    public void tearDown() {
        InitialConfigurator.DELAYED_HANDLER.setHandlers(new Handler[] {});
    }

    @Test
    public void splunkErrorShouldBeLoggedToStderr() {
        SplunkErrorCallback callback = new SplunkErrorCallback(stdout, stderr);

        callback.error(Collections.singletonList(logEvent), new Exception("Test exception"));

        assertThat(errContent.toString(), containsString("Test exception"));
    }

    @Test
    public void originalLogShouldNotBeLoggedIfConsoleHandlerEnabled() {
        InitialConfigurator.DELAYED_HANDLER.addHandler(new ConsoleHandler());
        SplunkErrorCallback callback = new SplunkErrorCallback(stdout, stderr);

        callback.error(Collections.singletonList(logEvent), new Exception("Test exception"));

        verifyNoInteractions(stdout);
    }

    @Test
    public void originalLogShouldNotBeLoggedIfConsoleHandlerLevelOff() {
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.OFF);
        InitialConfigurator.DELAYED_HANDLER.addHandler(handler);
        SplunkErrorCallback callback = new SplunkErrorCallback(stdout, stderr);

        callback.error(Collections.singletonList(logEvent), new Exception("Test exception"));

        verify(stdout).println("Hello");
    }

    @Test
    public void originalLogShouldBeLoggedToStdoutIfConsoleHandlerDisabled() {
        SplunkErrorCallback callback = new SplunkErrorCallback(stdout, stderr);

        callback.error(Collections.singletonList(logEvent), new Exception("Test exception"));

        verify(stdout).println("Hello");
    }
}
