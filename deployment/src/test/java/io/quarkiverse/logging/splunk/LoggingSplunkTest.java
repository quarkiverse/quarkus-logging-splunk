/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;
import static org.mockserver.model.RegexBody.regex;

import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.jboss.logging.MDC;
import org.jboss.logmanager.handlers.DelayedHandler;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.ClearType;

import io.quarkus.bootstrap.logging.InitialConfigurator;
import io.quarkus.test.QuarkusUnitTest;

class LoggingSplunkTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withConfigurationResource("application-splunk-logging-default.properties")
            .withConfigurationResource("mock-server.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    static ClientAndServer httpServer;

    static final org.jboss.logging.Logger logger = org.jboss.logging.Logger.getLogger(LoggingSplunkTest.class);

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
    public void handlerShouldBeCreated() {
        DelayedHandler delayedHandler = InitialConfigurator.DELAYED_HANDLER;
        assertThat(Logger.getLogger("").getHandlers(), hasItemInArray(delayedHandler));
        Handler handler = Arrays.stream(delayedHandler.getHandlers())
                .filter(h -> (h instanceof SplunkLogHandler))
                .findFirst().orElse(null);
        assertThat(handler, notNullValue());
    }

    @Test
    public void handlerShouldFormatMessage() {
        logger.infov("hello {0}", "splunk!");
        httpServer.verify(request().withBody(json("{ event: { message: 'hello splunk!' }}")));
    }

    @Test
    public void eventIsAJsonObjectWithMetadata() {
        logger.info("hello splunk");
        httpServer.verify(request().withBody(json("{ event: { message: 'hello splunk' }}")));
    }

    @Test
    public void eventHasStandardMetadata() {
        logger.info("hello splunk");
        httpServer.verify(request().withBody(json(
                "{ source: 'mysource', sourcetype: 'mysourcetype', index: 'myindex'} "))
                .withBody(regex(".*host.*")));
    }

    @Test
    public void tokenIsSentAsAuthorizationHeader() {
        logger.info("hello splunk");
        httpServer.verify(request().withHeader("Authorization", "Splunk 12345678-1234-1234-1234-1234567890AB"));
    }

    @Test
    public void clientAddsSomePredefinedMetadata() {
        logger.info("hello splunk");
        httpServer.verify(request().withBody(json(
                "{ event: { message: 'hello splunk', severity:'INFO', logger:'io.quarkiverse.logging.splunk.LoggingSplunkTest' }}")));
    }

    @Test
    public void mdcFieldsShouldBeSentAsMetadata() {
        MDC.put("mdc-key", "mdc-value");
        logger.info("hello mdc");
        httpServer.verify(
                request().withBody(json("{ event: { message: 'hello mdc', properties: { 'mdc-key': 'mdc-value' }}}")));
    }
}
