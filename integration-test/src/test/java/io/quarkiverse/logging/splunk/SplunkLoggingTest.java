/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.quarkiverse.logging.splunk.test.LoggingSplunkApiUrl;
import io.quarkiverse.logging.splunk.test.LoggingSplunkInjectingTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

@QuarkusTest
@QuarkusTestResource(LoggingSplunkInjectingTestResource.class)
public class SplunkLoggingTest {
    @LoggingSplunkApiUrl
    String splunkApiUrl;

    @Test
    public void test() throws InterruptedException {
        RestAssured.given().when().get("/log-to-splunk").then().statusCode(NO_CONTENT.getStatusCode());
        Thread.sleep(2000);

        searchSplunk("hello splunk").then().statusCode(200).body(containsString("hello splunk"), containsString("mdc-value"),
                containsString("myValue"));
        searchSplunk("********* log").then().statusCode(200);
    }

    private Response searchSplunk(String log) {
        // XML REST API - see https://docs.splunk.com/Documentation/Splunk/latest/RESTREF/RESTsearch#search.2Fjobs
        // Note: we can't assert on fields, which require 2 calls: GET /services/search/jobs and GET /services/search/jobs/<id>
        return RestAssured.given()
                .request()
                .formParam("search", "search \"" + log + "\"")
                .formParam("exec_mode", "oneshot")
                //.formParam("output_mode", "json")
                .relaxedHTTPSValidation()
                .auth().basic("admin", "admin123")
                .log().all()
                .post(splunkApiUrl + "/services/search/jobs");
    }
}
