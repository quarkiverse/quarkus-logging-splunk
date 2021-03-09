/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import static org.testcontainers.containers.wait.strategy.Wait.*;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class SplunkResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger logger = LoggerFactory.getLogger(SplunkResource.class);

    private final GenericContainer splunk = new GenericContainer(
            "splunk/splunk")
                    .withExposedPorts(8000, 8088, 8089)
                    .withEnv("SPLUNK_START_ARGS", "--accept-license")
                    .withEnv("SPLUNK_PASSWORD", "admin123")
                    .withEnv("SPLUNK_HEC_TOKEN", "29fe2838-cab6-4d17-a392-37b7b8f41f75")
                    .waitingFor(forLogMessage(".*Ansible playbook complete.*\\n", 1))
                    .withStartupTimeout(Duration.ofMinutes(2));

    @Override
    public Map<String, String> start() {
        logger.info("Starting splunk docker container");
        splunk.start();
        logger.info("Splunk docker container started");
        SplunkLoggingTest.splunkAPIPort = splunk.getMappedPort(8089);
        SplunkLoggingTest.dockerHost = splunk.getHost();
        return Collections.singletonMap("quarkus.log.handler.splunk.url",
                "https://" + splunk.getHost() + ":" + splunk.getMappedPort(8088));
    }

    @Override
    public void stop() {
        splunk.stop();
    }
}
