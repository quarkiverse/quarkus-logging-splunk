/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.jboss.logmanager.ExtHandler;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.test.QuarkusUnitTest;

class LoggingSplunkDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withConfigurationResource("application-splunk-logging-disabled.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    @Test
    void extensionDisabled() {
        ExtHandler delayedHandler = InitialConfigurator.DELAYED_HANDLER;
        assertThat(Logger.getLogger("").getHandlers(), hasItemInArray(delayedHandler));
        Handler handler = Arrays.stream(delayedHandler.getHandlers())
                .filter(h -> (h instanceof SplunkLogHandler))
                .findFirst().orElse(null);
        assertNull(handler);
    }
}
