/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.response.Response;

@QuarkusTest
@QuarkusTestResource(SplunkResource.class)
public class SplunkLoggingTest {

    static int splunkAPIPort = 0;

    static String dockerHost = null;

    @Test
    public void test() throws InterruptedException {
        assertTrue(splunkAPIPort > 0);
        assertNotNull(dockerHost);
        RestAssured.given().when().get("/log-to-splunk").then().statusCode(NO_CONTENT.getStatusCode());
        Thread.sleep(2000);

        searchSplunk("hello splunk").then().statusCode(200).body(containsString("hello splunk"), containsString("mdc-value"));
        searchSplunk("********* log").then().statusCode(200);
    }

    private static Response searchSplunk(String log) {
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
                .post("https://" + dockerHost + ":" + splunkAPIPort + "/services/search/jobs");
    }

}
