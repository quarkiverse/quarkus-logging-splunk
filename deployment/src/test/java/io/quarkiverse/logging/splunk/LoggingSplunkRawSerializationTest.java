package io.quarkiverse.logging.splunk;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class LoggingSplunkRawSerializationTest extends AbstractMockServerTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withConfigurationResource("application-splunk-logging-raw.properties")
            .withConfigurationResource("mock-server.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    static final org.jboss.logging.Logger logger = org.jboss.logging.Logger.getLogger(LoggingSplunkRawSerializationTest.class);

    @Test
    void sendsTheRawEvent() {
        logger.warn("hello splunk");
        awaitMockServer();
        httpServer.verify(requestToRawEndpoint().withBody("hello splunk"));
    }

    @Test
    void sendsMetadata() {
        logger.warn("hello splunk");
        awaitMockServer();
        httpServer.verify(requestToRawEndpoint()
                .withQueryStringParameter("index", "myindex")
                .withQueryStringParameter("source", "mysource")
                .withQueryStringParameter("sourcetype", "mysourcetype"));
    }

    @Test
    void sendsAuthenticationHeader() {
        logger.warn("hello splunk");
        awaitMockServer();
        httpServer.verify(requestToRawEndpoint().withHeader(
                "Authorization", "Splunk 12345678-1234-1234-1234-1234567890AB"));
    }

}
