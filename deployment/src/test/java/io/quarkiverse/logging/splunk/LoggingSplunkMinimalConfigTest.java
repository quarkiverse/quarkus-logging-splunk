/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;
import static org.mockserver.model.Not.not;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.ClearType;

import io.quarkus.test.QuarkusUnitTest;

class LoggingSplunkMinimalConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withConfigurationResource("application-splunk-logging-minimal.properties")
            .withConfigurationResource("mock-server.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    static ClientAndServer httpServer;

    static final Logger logger = Logger.getLogger(LoggingSplunkMinimalConfigTest.class);

    @BeforeAll
    public static void setUpOnce() {
        httpServer = ClientAndServer.startClientAndServer(8088);
        // This needs to be done as early as possible, so Quarkus startup logs don't fail to be sent
        httpServer
                .when(request().withPath("/services/collector/event/1.0"))
                .respond(response().withStatusCode(200).withBody("{}"));
        httpServer.when(request()).respond(response().withStatusCode(400));
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
    void indexIsNotSentIfUnspecified() {
        logger.info("hello splunk");
        httpServer.verify(request().withBody(not(json("{ index: ''}"))));
    }

    @Test
    void sourceTypeDefaultsToJson() {
        logger.info("hello splunk");
        httpServer.verify(request().withBody(json("{ sourcetype: '_json'}")));
    }
}
