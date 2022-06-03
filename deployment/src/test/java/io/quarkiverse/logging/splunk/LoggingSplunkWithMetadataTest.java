/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import static org.mockserver.model.JsonBody.json;
import static org.mockserver.model.RegexBody.regex;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class LoggingSplunkWithMetadataTest extends AbstractMockServerTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withConfigurationResource("application-splunk-logging-with-metadata.properties")
            .withConfigurationResource("mock-server.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    static final Logger logger = Logger.getLogger(LoggingSplunkWithMetadataTest.class);

    @Test
    void clientAddsStructuredMetadata() {
        logger.error("hello splunk", new RuntimeException("test exception"));
        awaitMockServer();
        httpServer.verify(requestToJsonEndpoint().withBody(json(
                "{ event: { message: 'hello splunk', " +
                        "logger:'io.quarkiverse.logging.splunk.LoggingSplunkWithMetadataTest', " +
                        "exception: 'test exception' }}"))
                .withBody(regex(".*thread.*")));
    }

    @Test
    void clientAddsAdditionalMetadataFields() {
        logger.info("hello splunk");
        awaitMockServer();
        httpServer.verify(requestToJsonEndpoint().withBody(
                json(
                        "{ fields: { "
                                + "additional-field-0: 'important-information', "
                                + "additional-field-1: 'less-important-information'"
                                + "}"
                                + "}")));
    }

}
