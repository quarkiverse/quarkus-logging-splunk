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
import org.mockserver.verify.VerificationTimes;

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
    void handlerShouldBeCreated() {
        DelayedHandler delayedHandler = InitialConfigurator.DELAYED_HANDLER;
        assertThat(Logger.getLogger("").getHandlers(), hasItemInArray(delayedHandler));
        Handler handler = Arrays.stream(delayedHandler.getHandlers())
                .filter(h -> (h instanceof SplunkLogHandler))
                .findFirst().orElse(null);
        assertThat(handler, notNullValue());
    }

    @Test
    void handlerShouldFormatMessage() {
        logger.warnv("hello {0}", "splunk!");
        httpServer.verify(request().withBody(json("{ event: { message: 'hello splunk!' }}")));
    }

    @Test
    void eventIsAJsonObjectWithMetadata() {
        logger.warn("hello splunk");
        httpServer.verify(request().withBody(json("{ event: { message: 'hello splunk' }}")));
    }

    @Test
    void eventHasStandardMetadata() {
        logger.warn("hello splunk");
        httpServer.verify(request().withBody(json(
                "{ source: 'mysource', sourcetype: 'mysourcetype', index: 'myindex'} "))
                .withBody(regex(".*host.*")));
    }

    @Test
    void tokenIsSentAsAuthorizationHeader() {
        logger.warn("hello splunk");
        httpServer.verify(request().withHeader("Authorization", "Splunk 12345678-1234-1234-1234-1234567890AB"));
    }

    @Test
    void clientAddsMinimalMetadata() {
        logger.warn("hello splunk");
        httpServer.verify(request().withBody(json(
                "{ event: { message: 'hello splunk', severity:'WARN' }}")));
    }

    @Test
    void mdcFieldsShouldBeSentAsMetadata() {
        MDC.put("mdc-key", "mdc-value");
        logger.warn("hello mdc");
        httpServer.verify(
                request().withBody(json("{ event: { message: 'hello mdc', properties: { 'mdc-key': 'mdc-value' }}}")));
    }

    @Test
    void messageShouldContainException() {
        logger.error("unexpected error", new RuntimeException("test exception"));
        httpServer.verify(request()
                .withBody(regex(".*unexpected error: java.lang.RuntimeException: test exception.*")));
    }

    @Test
    void logLevelShouldBeUsed() {
        logger.info("Info log");
        httpServer.verify(request().withBody(json("{ event: { message: 'Info log' }}")), VerificationTimes.exactly(0));
    }
}
