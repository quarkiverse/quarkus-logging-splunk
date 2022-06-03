/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import static org.mockserver.model.JsonBody.json;
import static org.mockserver.model.Not.not;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class LoggingSplunkMinimalConfigTest extends AbstractMockServerTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withConfigurationResource("application-splunk-logging-minimal.properties")
            .withConfigurationResource("mock-server.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    static final Logger logger = Logger.getLogger(LoggingSplunkMinimalConfigTest.class);

    @Test
    void indexIsNotSentIfUnspecified() {
        logger.info("hello splunk");
        awaitMockServer();
        httpServer.verify(requestToJsonEndpoint().withBody(not(json("{ index: ''}"))));
    }

    @Test
    void sourceTypeDefaultsToJson() {
        logger.info("hello splunk");
        awaitMockServer();
        httpServer.verify(requestToJsonEndpoint().withBody(json("{ sourcetype: '_json'}")));
    }
}
