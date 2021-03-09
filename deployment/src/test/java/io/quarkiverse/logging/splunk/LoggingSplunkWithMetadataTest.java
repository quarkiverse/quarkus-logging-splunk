/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;
import static org.mockserver.model.RegexBody.regex;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;

import io.quarkus.test.QuarkusUnitTest;

class LoggingSplunkWithMetadataTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withConfigurationResource("application-splunk-logging-with-metadata.properties")
            .withConfigurationResource("mock-server.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    static ClientAndServer httpServer;

    static final Logger logger = Logger.getLogger(LoggingSplunkWithMetadataTest.class);

    @BeforeAll
    public static void setUpOnce() {
        httpServer = ClientAndServer.startClientAndServer(8088);
        // This needs to be done as early as possible, so Quarkus startup logs don't fail to be sent
        httpServer
                .when(request().withPath("/services/collector/event/1.0"))
                .respond(response().withStatusCode(200).withBody("{}"));
        httpServer.when(request()).respond(response().withStatusCode(400));
    }

    @Test
    void clientAddsStructuredMetadata() {
        logger.error("hello splunk", new RuntimeException("test exception"));
        httpServer.verify(request().withBody(json(
                "{ event: { message: 'hello splunk', " +
                        "logger:'io.quarkiverse.logging.splunk.LoggingSplunkWithMetadataTest', " +
                        "exception: 'test exception' }}"))
                .withBody(regex(".*thread.*")));
    }
}
