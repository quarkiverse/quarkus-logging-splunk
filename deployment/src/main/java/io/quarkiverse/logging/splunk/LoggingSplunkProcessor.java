/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import java.util.Collection;

import org.jboss.jandex.ClassInfo;

import com.splunk.logging.HttpEventCollectorMiddleware;
import com.splunk.logging.HttpEventCollectorSender;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.deployment.builditem.NamedLogHandlersBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

class LoggingSplunkProcessor {

    private static final String FEATURE = "logging-splunk";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    LogHandlerBuildItem logHandler(SplunkLogHandlerRecorder recorder, SplunkConfig config) {
        return new LogHandlerBuildItem(recorder.initializeHandler(config));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    NamedLogHandlersBuildItem logNamedHandlers(SplunkLogHandlerRecorder recorder, SplunkConfig config) {
        return new NamedLogHandlersBuildItem(recorder.initializeHandlers(config));
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem enableSSL() {
        // Enable SSL support by default
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitialization() {
        return new RuntimeInitializedClassBuildItem(HttpEventCollectorSender.class.getCanonicalName());
    }

    @BuildStep
    public void configureNativeExecutable(CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        // HttpSenderMiddleware can be selected via the middleware configuration and is loaded
        // dynamically, so we need to make sure it is registered for reflection.
        Collection<ClassInfo> messages = combinedIndex.getIndex().getAllKnownSubclasses(
                HttpEventCollectorMiddleware.HttpSenderMiddleware.class);
        for (ClassInfo message : messages) {
            reflectiveClass.produce(ReflectiveClassBuildItem.builder(message.name().toString())
                    .constructors()
                    .build());
        }
    }
}
