package io.quarkiverse.logging.splunk.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.quarkus.test.common.DevServicesContext;

@ExtendWith(MockitoExtension.class)
class LoggingSplunkInjectingTestResourceTest {
    @Mock
    DevServicesContext context;

    LoggingSplunkInjectingTestResource resource = new LoggingSplunkInjectingTestResource();

  @BeforeEach
  void setUp() {
    when(context.devServicesProperties()).thenReturn(Map.of());
  }

    @Test
    void disabled() {
        FieldWithAnnotation testInstance = new FieldWithAnnotation();

        resource.setIntegrationTestContext(context);
        resource.inject(testInstance);

        assertThat(testInstance.splunkApiUrl, nullValue());
        assertThat(testInstance.splunkHandlerUrl, nullValue());
    }

  @Test
   void enabled() {
     when(context.devServicesProperties()).thenReturn(Map.of("quarkus.log.handler.splunk.url",
         "http://localhost:8088", "quarkus.log.handler.splunk.devservices.api-url", "http://localhost:8089"));
    FieldWithAnnotation testInstance = new FieldWithAnnotation();

     resource.setIntegrationTestContext(context);
     resource.inject(testInstance);

     assertThat(testInstance.splunkApiUrl, equalTo("http://localhost:8089"));
     assertThat(testInstance.splunkHandlerUrl, equalTo("http://localhost:8088"));
   }

  @Test
  void noAnnotationORWrongType() {
    when(context.devServicesProperties()).thenReturn(Map.of("quarkus.log.handler.splunk.url",
        "http://localhost:8088", "quarkus.log.handler.splunk.devservices.api-url", "http://localhost:8089"));
    FieldWithNoAnnotation testInstance = new FieldWithNoAnnotation();

    resource.setIntegrationTestContext(context);
    resource.inject(testInstance);

    assertThat(testInstance.splunkApiUrl, nullValue());
    assertThat(testInstance.splunkHandlerUrl, nullValue());
  }

    public static class NoFields {

    }

    public static class FieldWithNoAnnotation {
        String splunkApiUrl;
        @LoggingSplunkHandlerUrl
        Integer splunkHandlerUrl;
    }

    public static class FieldWithAnnotation {
        @LoggingSplunkApiUrl
        String splunkApiUrl;
        @LoggingSplunkHandlerUrl
        String splunkHandlerUrl;
    }

}