package io.quarkiverse.logging.splunk;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.ClearType;
import org.mockserver.model.HttpRequest;

import io.quarkus.test.QuarkusUnitTest;

public abstract class AbstractMockServerTest {

    static ClientAndServer httpServer;

    @BeforeAll
    public static void setUpOnce() {
        ConfigurationProperties.dynamicallyCreateCertificateAuthorityCertificate(false);
        ConfigurationProperties.assumeAllRequestsAreHttp(true);
        httpServer = ClientAndServer.startClientAndServer(8088);
        // This needs to be done as early as possible, so Quarkus startup logs don't fail to be sent
        httpServer
                .when(request().withPath("/services/collector/event/1.0"))
                .respond(response().withStatusCode(200).withBody("{}"));
        httpServer
                .when(request().withPath("/services/collector/raw"))
                .respond(response().withStatusCode(200).withBody("{}"));
        httpServer.when(request()).respond(response().withStatusCode(400));
    }

    @AfterAll
    public static void tearDownOnce() {
        httpServer.stop();
    }

    @AfterEach
    public void tearDown() {
        httpServer.clear(request(), ClearType.LOG);
    }

    /**
     * Splunk client uses an asynchronous HTTP queue, so to avoid race conditions we need to wait for mockserver to receive the
     * call.
     */
    protected void awaitMockServer() {
        await().atMost(1, SECONDS).until(() -> httpServer.retrieveRecordedRequests(request()).length != 0);
    }

    protected HttpRequest requestToJsonEndpoint() {
        return request().withPath("/services/collector/event/1.0");
    }

    protected HttpRequest requestToRawEndpoint() {
        return request().withPath("/services/collector/raw");
    }

    /**
     * QuarkusUnitTest 3.16+ only supports one call to #withConfigurationResource
     * So use #overrideConfigKey instead of a properties.
     * See https://github.com/quarkusio/quarkus/issues/43914
     */
    protected static QuarkusUnitTest withMockServerConfig() {

        return new QuarkusUnitTest()
                // Switch from HTTPS to HTTP
                .overrideConfigKey("quarkus.log.handler.splunk.url", "http://localhost:8088")
                // Avoid infinite loop of logging via splunk handler, mockserver must only log to stdout !
                .overrideConfigKey("quarkus.log.handler.console.\"stdout\".format", "%s%e%n")
                .overrideConfigKey("quarkus.log.category.\"org.mockserver\".handlers", "stdout")
                .overrideConfigKey("quarkus.log.category.\"org.mockserver\".use-parent-handlers", "false")
                // Avoid batching and send events immediately, to make unit tests more synchronous
                // Note that OKHttp client still executes its I/O on a separate thread
                .overrideConfigKey("quarkus.log.handler.splunk.batch-interval", "0")
                .overrideConfigKey("quarkus.log.handler.splunk.batch-size-bytes", "0")
                .overrideConfigKey("quarkus.log.handler.splunk.batch-size-count", "0")
                .overrideConfigKey("quarkus.log.handler.splunk.send-mode", "sequential");
    }
}
