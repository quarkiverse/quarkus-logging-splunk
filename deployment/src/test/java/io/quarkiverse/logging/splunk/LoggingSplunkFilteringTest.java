/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import static org.mockserver.model.JsonBody.json;
import static org.mockserver.model.RegexBody.regex;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

import org.jboss.logging.Logger;
import org.jboss.logmanager.Level;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.logging.LoggingFilter;
import io.quarkus.test.QuarkusUnitTest;

class LoggingSplunkFilteringTest extends AbstractMockServerTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withConfigurationResource("application-splunk-logging-filtering.properties")
            .withConfigurationResource("mock-server.properties")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class));

    static final Logger logger = Logger.getLogger(LoggingSplunkFilteringTest.class);

    @Test
    void filterShouldBeCalled() {
        logger.info("hello splunk");
        awaitMockServer();
        httpServer.verify(
                requestToJsonEndpoint().withBody(regex(".*hello splunk.*")).withBody(json("{ event: { severity:'ERROR' }}")));
    }

    @LoggingFilter(name = "my-filter")
    public static class MyFilter implements Filter {

        @Override
        public boolean isLoggable(LogRecord record) {
            record.setLevel(Level.ERROR);
            return true;
        }
    }
}
