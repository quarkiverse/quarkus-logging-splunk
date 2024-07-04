package io.quarkiverse.logging.splunk.test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class LoggingSplunkInjectingTestResource
        implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {
    private static final Logger log = Logger.getLogger(LoggingSplunkInjectingTestResource.class);

    private static final String HANDLER_URL_CONFIG_PROP = "quarkus.log.handler.splunk.url";

    private static final String API_URL_CONFIG_PROP = "quarkus.log.handler.splunk.devservices.api-url";

    private String handlerUrl;

    private String apiUrl;

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        handlerUrl = context.devServicesProperties().get(HANDLER_URL_CONFIG_PROP);
        apiUrl = context.devServicesProperties().get(API_URL_CONFIG_PROP);
        if (handlerUrl == null || apiUrl == null) {
            log.warnf("Did not receive any {} and/or {} property values from the DevServiceContext!", HANDLER_URL_CONFIG_PROP,
                    API_URL_CONFIG_PROP);
        }
    }

    @Override
    public Map<String, String> start() {
        return Map.of();
    }

    @Override
    public void stop() {

    }

    @Override
    public void inject(Object testInstance) {
        for (Field field : getFields(testInstance.getClass(), true)) {
            field.setAccessible(true);
            Object currentValue = getValue(field, testInstance);
            if (currentValue == null) {
                inject(field, testInstance, handlerUrl, LoggingSplunkHandlerUrl.class);
                inject(field, testInstance, apiUrl, LoggingSplunkApiUrl.class);
            }
        }
    }

    private Set<Field> getFields(Class<?> klazz, boolean isInstanceClass) {
        if (Object.class.equals(klazz)) {
            return new HashSet<>();
        }
        Set<Field> set = getFields(klazz.getSuperclass(), false);
        for (Field field : klazz.getDeclaredFields()) {
            if (isInstanceClass || !Modifier.isPrivate(field.getModifiers())) {
                set.add(field);
            }
        }
        return set;
    }

    private static Object getValue(Field field, Object testInstance) {
        try {
            return field.get(testInstance);
        } catch (IllegalAccessException e) {
            log.warn("Could not get value from field {}", field, e);
        }
        return null;
    }

    private static void inject(Field field, Object testInstance, String value,
            Class<? extends Annotation> annotationClass) {
        if (field.getType().isAssignableFrom(String.class) && field.isAnnotationPresent(annotationClass)) {
            try {
                field.set(testInstance, value);
            } catch (IllegalAccessException e) {
                log.warnf("Could not inject {} into {}", value, testInstance.getClass(), e);
            }
        }
    }
}
