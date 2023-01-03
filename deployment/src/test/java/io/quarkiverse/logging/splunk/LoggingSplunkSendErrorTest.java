/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.ClearType;
import org.mockserver.verify.VerificationTimes;

import io.quarkus.test.QuarkusUnitTest;

@Disabled("This test is not so important, and there's no clear evidence of " +
        "why it's flaky and may come from the QuarkusUnitTest harness")
class LoggingSplunkSendErrorTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withConfigurationResource("application-splunk-logging-failure.properties")
            .withConfigurationResource("mock-server.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    static final Logger logger = Logger.getLogger(LoggingSplunkSendErrorTest.class);

    static ClientAndServer httpServer;

    @BeforeAll
    public static void setUpOnce() {
        httpServer = ClientAndServer.startClientAndServer(8088);
        // Reject all requests (ex: wrong token, ...)
        httpServer.when(request()).respond(response().withStatusCode(401));
    }

    @AfterAll
    public static void tearDownOnce() {
        httpServer.stop();
    }

    @AfterEach
    public void tearDown() {
        httpServer.clear(request(), ClearType.LOG);
    }

    @Test
    void testSendError() {
        logger.info("error splunk");
        await().atMost(1, SECONDS).until(() -> httpServer.retrieveRecordedRequests(request()).length != 0);
        // The retries-on-error is not applicable in case of HTTP error
        httpServer.verify(request().withBody(json("{ event: { message:'error splunk'} }")),
                VerificationTimes.exactly(1));
    }
}
