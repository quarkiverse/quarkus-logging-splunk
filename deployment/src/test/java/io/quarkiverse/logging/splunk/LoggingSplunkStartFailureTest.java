/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * No mock server
 */
public class LoggingSplunkStartFailureTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withConfigurationResource("application-splunk-logging-failure.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    static final Logger logger = Logger.getLogger(LoggingSplunkStartFailureTest.class);

    @Test
    public void testSendError() throws InterruptedException {
        logger.info("error starting splunk");
        // Wait for connection timeout
        Thread.sleep(5000);
    }
}
