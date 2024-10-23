package io.quarkiverse.logging.splunk;

import static org.mockserver.model.JsonBody.json;
import static org.mockserver.model.RegexBody.regex;

import org.jboss.logging.MDC;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class LoggingSplunkFlatSerializationTest extends AbstractMockServerTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = withMockServerConfig()
            .withConfigurationResource("application-splunk-logging-flat.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    static final org.jboss.logging.Logger logger = org.jboss.logging.Logger.getLogger(LoggingSplunkFlatSerializationTest.class);

    @Test
    void handlerShouldFormatMessage() {
        logger.warnv("hello {0}", "splunk!");
        awaitMockServer();
        httpServer.verify(requestToJsonEndpoint().withBody(json("{ event: 'hello splunk!' }")));
    }

    @Test
    void eventHasStandardMetadata() {
        logger.warn("hello splunk");
        awaitMockServer();
        httpServer.verify(requestToJsonEndpoint().withBody(json(
                "{ source: 'mysource', index: 'myindex'} "))
                .withBody(regex(".*host.*")));
    }

    @Test
    void tokenIsSentAsAuthorizationHeader() {
        logger.warn("hello splunk");
        awaitMockServer();
        httpServer
                .verify(requestToJsonEndpoint().withHeader("Authorization", "Splunk 12345678-1234-1234-1234-1234567890AB"));
    }

    @Test
    void eventHasSeverityMetadata() {
        logger.warn("hello splunk");
        awaitMockServer();
        httpServer.verify(requestToJsonEndpoint().withBody(json("{ fields: { 'level': 'WARN' } }")));
    }

    @Test
    void eventHasOptionalMetadata() {
        logger.error("hello splunk", new RuntimeException("test exception"));
        awaitMockServer();
        httpServer.verify(requestToJsonEndpoint().withBody(regex(".*hello splunk.*")));
        httpServer.verify(requestToJsonEndpoint().withBody(json(
                "{ fields: { logger:'io.quarkiverse.logging.splunk.LoggingSplunkFlatSerializationTest', exception: 'test exception' }}")));
        httpServer.verify(requestToJsonEndpoint().withBody(regex(".*thread.*")));
    }

    @Test
    void mdcFieldsShouldBeSentAsMetadata() {
        MDC.put("mdc-key", "mdc-value");
        logger.warn("hello mdc");
        MDC.remove("mdc-key");
        awaitMockServer();
        httpServer.verify(
                requestToJsonEndpoint()
                        .withBody(json("{ event: 'hello mdc', fields: { 'mdc-key': 'mdc-value' }}")));
    }

    @Test
    void staticMetadataFields() {
        logger.warn("hello splunk");
        awaitMockServer();
        httpServer.verify(requestToJsonEndpoint().withBody(
                json("{ fields: { metadata-0: 'value0', metadata-1: 'value1' } }")));
    }

    @Test
    void nestedJsonMessageIsNotParsed() {
        logger.warn("{ 'greeting': 'hello', 'user': 'splunk' }");
        awaitMockServer();
        httpServer.verify(requestToJsonEndpoint().withBody(json("{ event: \"{ 'greeting': 'hello', 'user': 'splunk' }\" }")));
    }

}
