/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

@Path("/log-to-splunk")
@ApplicationScoped
public class SplunkHandlerResource {

    private static final Logger logger = Logger.getLogger(SplunkHandlerResource.class);

    @GET
    public void log() {
        MDC.put("mdc-key", "mdc-value");
        logger.info("hello splunk");
        logger.info("Sensitive log");
        MDC.remove("mdc-key");
    }
}
