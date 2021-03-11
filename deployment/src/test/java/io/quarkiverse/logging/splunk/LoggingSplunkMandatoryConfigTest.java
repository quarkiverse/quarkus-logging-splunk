/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class LoggingSplunkMandatoryConfigTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("quarkus.log.handler.splunk.enabled=true"), "application.properties"))
            .assertException(e -> {
                Assertions.assertSame(IllegalArgumentException.class, e.getClass());
                Assertions.assertTrue(e.getMessage().contains("quarkus.log.handler.splunk.token"));
            });

    @Test
    public void missingTokenThrowsIllegalArgumentException() {
        assertTrue("We must fail on missing token" == null);
    }
}
