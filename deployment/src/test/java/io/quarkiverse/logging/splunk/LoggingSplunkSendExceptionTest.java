/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import static org.mockserver.model.HttpError.error;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.JsonBody.json;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.verify.VerificationTimes;

import io.quarkus.test.QuarkusUnitTest;

class LoggingSplunkSendExceptionTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withConfigurationResource("application-splunk-logging-failure.properties")
            .withConfigurationResource("mock-server.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    static final Logger logger = Logger.getLogger(LoggingSplunkSendExceptionTest.class);

    static ClientAndServer httpServer;

    @BeforeAll
    public static void setUpOnce() {
        httpServer = ClientAndServer.startClientAndServer(8088);
        // Drop connections to trigger I/O exceptions in HTTP client
        httpServer.when(request()).error(error().withDropConnection(true));
    }

    @AfterAll
    public static void tearDownOnce() {
        httpServer.stop();
    }

    @Test
    void testSendError() throws InterruptedException {
        logger.info("error starting splunk");
        // HTTP client connections happens on a separate thread.
        Thread.sleep(5000);
        // Should be retried at least once (actually more, maybe happens at a lower level).
        httpServer.verify(request().withBody(json("{ event: { message:'error starting splunk'} }")),
                VerificationTimes.atLeast(2));
    }
}
