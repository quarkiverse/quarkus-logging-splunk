package io.quarkiverse.logging.splunk;

import static org.mockserver.model.JsonBody.json;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class LoggingSplunkAsyncTest extends AbstractMockServerTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = withMockServerConfig()
            .withConfigurationResource("application-splunk-logging-async.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    static final Logger logger = Logger.getLogger(LoggingSplunkAsyncTest.class);

    @Test
    void eventIsAJsonObjectWithMetadata() {
        logger.warn("hello splunk");
        awaitMockServer();
        httpServer.verify(requestToJsonEndpoint().withBody(json("{ event: { message: 'hello splunk' }}")));
    }
}
