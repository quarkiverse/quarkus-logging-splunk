/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class LoggingSplunkMandatoryConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .withConfigurationResource("application-splunk-logging-mandatory.properties")
            .assertException(e -> {
                assertSame(IllegalArgumentException.class, e.getClass());
                assertTrue(e.getMessage().contains("quarkus.log.handler.splunk.token"));
            });

    @Test
    void missingTokenThrowsIllegalArgumentException() {
        fail("Bootstrap should have failed due to missing token");
    }
}
