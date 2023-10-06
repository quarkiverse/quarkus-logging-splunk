/*
Copyright (c) 2023 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio, Yohann Puyhaubert (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import io.quarkus.test.QuarkusUnitTest;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.mockserver.model.JsonBody.json;

class LoggingSplunkNamedHandlerConfigTest extends AbstractMockServerTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withConfigurationResource("application-splunk-logging-named-handler.properties")
            .withConfigurationResource("mock-server.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    static final Logger logger = Logger.getLogger(LoggingSplunkNamedHandlerConfigTest.class);

    static final Logger monitoringLogger = Logger.getLogger("monitoring");

    @Test
    void indexWithDefaultLoggerAndNamedLogger() {
        logger.warn("hello splunk");
        monitoringLogger.info("{\"key\":\"value\"}");
        awaitMockServer();
        httpServer.verify(requestToJsonEndpoint()
                .withBody(json("{ index: 'mylogindex'}"))
                .withHeader("Authorization", "Splunk 12345678-1234-1234-1234-1234567890AB"));
        httpServer.verify(requestToJsonEndpoint()
                .withBody(json("{ index: 'mystatsindex'}"))
                .withHeader("Authorization", "Splunk 12345678-0000-0000-0000-1234567890AB"));
    }

}
