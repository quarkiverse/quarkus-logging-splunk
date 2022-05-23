package io.quarkiverse.logging.splunk;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.ClearType;
import org.mockserver.model.HttpRequest;

public abstract class AbstractMockServerTest {

    static ClientAndServer httpServer;

    @BeforeAll
    public static void setUpOnce() {
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

}
