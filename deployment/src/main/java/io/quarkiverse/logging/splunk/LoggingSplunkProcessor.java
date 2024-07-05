/*
Copyright (c) 2021 Amadeus s.a.s.
Contributor(s): Kevin Viet, Romain Quinio (Amadeus s.a.s.)
 */
package io.quarkiverse.logging.splunk;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Filter;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

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
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.logging.LoggingFilter;
import io.quarkus.runtime.logging.DiscoveredLogComponents;

class LoggingSplunkProcessor {

    public static final DotName LOGGING_FILTER = DotName.createSimple(LoggingFilter.class.getName());

    private static final DotName FILTER = DotName.createSimple(Filter.class.getName());

    private static final String ILLEGAL_LOGGING_FILTER_USE_MESSAGE = "'@" + LoggingFilter.class.getName()
            + "' can only be used on classes that implement '"
            + Filter.class.getName() + "' and that are marked as final.";

    private static final String FEATURE = "logging-splunk";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    LogHandlerBuildItem logHandler(SplunkLogHandlerRecorder recorder, SplunkConfig config,
            CombinedIndexBuildItem combinedIndexBuildItem) {
        DiscoveredLogComponents discoveredLogComponents = discoverLogComponents(combinedIndexBuildItem.getIndex());
        return new LogHandlerBuildItem(recorder.initializeHandler(config, discoveredLogComponents));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    NamedLogHandlersBuildItem logNamedHandlers(SplunkLogHandlerRecorder recorder, SplunkConfig config,
            CombinedIndexBuildItem combinedIndexBuildItem) {
        DiscoveredLogComponents discoveredLogComponents = discoverLogComponents(combinedIndexBuildItem.getIndex());
        return new NamedLogHandlersBuildItem(recorder.initializeHandlers(config, discoveredLogComponents));
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

    /**
     * Copied from io.quarkus.deployment.logging.LoggingResourceProcessor, as not exposed as a build item.
     */
    private DiscoveredLogComponents discoverLogComponents(IndexView index) {
        Collection<AnnotationInstance> loggingFilterInstances = index.getAnnotations(LOGGING_FILTER);
        DiscoveredLogComponents result = new DiscoveredLogComponents();

        Map<String, String> filtersMap = new HashMap<>();
        for (AnnotationInstance instance : loggingFilterInstances) {
            AnnotationTarget target = instance.target();
            if (target.kind() != AnnotationTarget.Kind.CLASS) {
                throw new IllegalStateException("Unimplemented mode of use of '" + LoggingFilter.class.getName() + "'");
            }
            ClassInfo classInfo = target.asClass();
            boolean isFilterImpl = false;
            ClassInfo currentClassInfo = classInfo;
            while ((currentClassInfo != null) && (!JandexUtil.DOTNAME_OBJECT.equals(currentClassInfo.name()))) {
                boolean hasFilterInterface = false;
                List<DotName> ifaces = currentClassInfo.interfaceNames();
                for (DotName iface : ifaces) {
                    if (FILTER.equals(iface)) {
                        hasFilterInterface = true;
                        break;
                    }
                }
                if (hasFilterInterface) {
                    isFilterImpl = true;
                    break;
                }
                currentClassInfo = index.getClassByName(currentClassInfo.superName());
            }
            if (!isFilterImpl) {
                throw new RuntimeException(
                        ILLEGAL_LOGGING_FILTER_USE_MESSAGE + " Offending class is '" + classInfo.name() + "'");
            }

            String filterName = instance.value("name").asString();
            if (filtersMap.containsKey(filterName)) {
                throw new RuntimeException("Filter '" + filterName + "' was defined multiple times.");
            }
            filtersMap.put(filterName, classInfo.name().toString());
        }
        if (!filtersMap.isEmpty()) {
            result.setNameToFilterClass(filtersMap);
        }

        return result;
    }
}
