/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.handlers.ConsoleHandler;

import com.splunk.logging.HttpEventCollectorErrorHandler.ErrorCallback;
import com.splunk.logging.HttpEventCollectorEventInfo;

import io.quarkus.bootstrap.logging.InitialConfigurator;

public class SplunkErrorCallback implements ErrorCallback {

    Boolean consoleEnabled;

    PrintStream stdout;

    PrintStream stderr;

    SplunkErrorCallback() {
        this(System.out, System.err); // NOSONAR
    }

    /**
     * For unit tests
     */
    SplunkErrorCallback(PrintStream stdout, PrintStream stderr) {
        this.stdout = stdout;
        this.stderr = stderr;
    }

    /**
     * Logs the original event to stdout (if console handler is disabled).
     * Logs the error to stderr.
     */
    @Override
    public void error(List<HttpEventCollectorEventInfo> list, Exception e) {
        final StringWriter stringWriter = new StringWriter();
        stringWriter.append("Error while sending events to Splunk HEC: ");
        stringWriter.append(e.getMessage()).append(System.lineSeparator());
        e.printStackTrace(new PrintWriter(stringWriter));
        this.stderr.println(stringWriter.toString());

        if (!isConsoleHandlerEnabled()) {
            for (HttpEventCollectorEventInfo logEvent : list) {
                this.stdout.println(logEvent.getMessage());
            }
        }
    }

    /**
     * This has to be determined lazily, as handlers are not yet registered when splunk is initialized.
     * An alternative was to check for config "quarkus.log.console.enable", but adds a microprofile-config dependency.
     */
    private boolean isConsoleHandlerEnabled() {
        if (consoleEnabled == null) {
            ExtHandler delayedHandler = InitialConfigurator.DELAYED_HANDLER;
            Handler consoleHandler = Arrays.stream(delayedHandler.getHandlers())
                    .filter(h -> (h instanceof ConsoleHandler))
                    .findFirst().orElse(null);
            consoleEnabled = (consoleHandler != null && !consoleHandler.getLevel().equals(Level.OFF));
        }
        return consoleEnabled;
    }
}
