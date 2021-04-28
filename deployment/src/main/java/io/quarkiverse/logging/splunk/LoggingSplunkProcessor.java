/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import com.splunk.logging.HttpEventCollectorSender;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
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
    ExtensionSslNativeSupportBuildItem enableSSL() {
        // Enable SSL support by default
        return new ExtensionSslNativeSupportBuildItem(FEATURE);
    }

    @BuildStep
    RuntimeInitializedClassBuildItem runtimeInitialization() {
        return new RuntimeInitializedClassBuildItem(HttpEventCollectorSender.class.getCanonicalName());
    }
}
