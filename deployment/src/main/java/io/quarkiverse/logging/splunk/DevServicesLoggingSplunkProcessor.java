package io.quarkiverse.logging.splunk;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.testcontainers.utility.DockerImageName;

import io.quarkiverse.logging.splunk.config.build.DevServicesLoggingSplunkBuildTimeConfig;
import io.quarkiverse.logging.splunk.config.build.SplunkBuildConfig;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.runtime.LaunchMode;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = { GlobalDevServicesConfig.Enabled.class })
public class DevServicesLoggingSplunkProcessor {
    private static final Logger log = Logger.getLogger(DevServicesLoggingSplunkProcessor.class);

    private static final String SPLUNK = "splunk";

    private static final String HANDLER_URL_CONFIG_PROP = "quarkus.log.handler.splunk.url";

    private static final String API_URL_CONFIG_PROP = "quarkus.log.handler.splunk.devservices.api-url";

    private static final String SPLUNK_LATEST = "registry-1.docker.io/splunk/splunk:latest";

    private static volatile DevServicesResultBuildItem.RunningDevService devService;

    private static volatile DevServicesLoggingSplunkBuildTimeConfig capturedDevServiceConfig;

    private static volatile boolean first = true;

    /**
     * Start one (or many in the future) SplunkContainer(s) depending on extension configuration. We also take care of
     * locating and re-using existing container if configured in shared mode.
     */
    @BuildStep
    public DevServicesResultBuildItem startSplunkContainer(LaunchModeBuildItem launchMode,
            DockerStatusBuildItem dockerStatusBuildItem,
            SplunkBuildConfig config,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            GlobalDevServicesConfig devServicesConfig) {

        // Figure out if we need to shut down and restart existing Splunk containers
        // if not and the Splunk containers have already started we just return
        if (devService != null) {
            if (config.devservices.equals(capturedDevServiceConfig)) {
                return devService.toBuildItem();
            }
            try {
                devService.close();
            } catch (Throwable e) {
                log.error("Failed to stop Splunk container", e);
            }
            devService = null;
            capturedDevServiceConfig = null;
        }

        // Re-initialize captured config and dev services.
        capturedDevServiceConfig = config.devservices;

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode.isTest() ? "(test) " : "") + "Splunk Dev Services Starting:", consoleInstalledBuildItem,
                loggingSetupBuildItem);
        try {
            devService = startContainer(config.devservices, dockerStatusBuildItem,
                    launchMode.getLaunchMode(), devServicesConfig.timeout);

            if (devService == null) {
                compressor.closeAndDumpCaptured();
                return null;
            } else {
                compressor.close();
                log.infof("The Splunk container is ready on %s", devService.getConfig().get(HANDLER_URL_CONFIG_PROP));
            }
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }

        if (first) {
            first = false;
            // Add close tasks on first run only.
            Runnable closeTask = () -> {
                if (devService != null) {
                    try {
                        devService.close();
                    } catch (Throwable t) {
                        log.error("Failed to stop Splunk", t);
                    }
                }
                first = true;
                devService = null;
                capturedDevServiceConfig = null;
            };
            closeBuildItem.addCloseTask(closeTask, true);
        }

        return devService.toBuildItem();
    }

    private DevServicesResultBuildItem.RunningDevService startContainer(DevServicesLoggingSplunkBuildTimeConfig config,
            DockerStatusBuildItem dockerStatusBuildItem, LaunchMode launchMode, Optional<Duration> timeout) {
        if (!isEnabled(config)) {
            // explicitly disabled
            log.info("Not starting devservices for Splunk as it has been disabled in the config");
            return null;
        }

        if (!dockerStatusBuildItem.isDockerAvailable()) {
            log.warn("Configure quarkus.log.handler.splunk.url or have a working docker daemon");
            return null;
        }

        DockerImageName dockerImageName = DockerImageName.parse(config.imageName.orElse(SPLUNK_LATEST))
                .asCompatibleSubstituteFor(SPLUNK_LATEST);

        SplunkContainer splunkContainer = new SplunkContainer(dockerImageName);

        // Add envs and timeout if provided.
        splunkContainer.withEnv(config.containerEnv);
        timeout.ifPresent(splunkContainer::withStartupTimeout);

        splunkContainer.start();

        return new DevServicesResultBuildItem.RunningDevService(SPLUNK, splunkContainer.getContainerId(),
                splunkContainer::close, getDevServiceExposedConfig(splunkContainer, config));
    }

    private Map<String, String> getDevServiceExposedConfig(SplunkContainer container,
            DevServicesLoggingSplunkBuildTimeConfig config) {
        final Map<String, String> exposedConfig = new HashMap<>();
        exposedConfig.put(HANDLER_URL_CONFIG_PROP, container.getSplunkHandlerUrl());
        exposedConfig.put(API_URL_CONFIG_PROP, container.getSplunkApiUrl());
        exposedConfig.put("quarkus.log.handler.splunk.token", SplunkContainer.HEC_TOKEN);
        exposedConfig.put("quarkus.log.handler.splunk.disable-certificate-validation", "true");
        exposedConfig.put("quarkus.log.handler.splunk.enabled", "true");
        config.plugNamedHandlers.forEach((k, v) -> {
            // Named handlers are configured using runtime configuration, which we do not have access to here
            // so a dedicated build time map was introduced to still be able to generate the configuration
            // to inject for each named handler.
            if (v) {
                exposedConfig.put("quarkus.log.handler.splunk." + k + ".url", container.getSplunkHandlerUrl());
                exposedConfig.put("quarkus.log.handler.splunk." + k + ".token", SplunkContainer.HEC_TOKEN);
                exposedConfig.put("quarkus.log.handler.splunk." + k + ".disable-certificate-validation", "true");
                exposedConfig.put("quarkus.log.handler.splunk." + k + ".enabled", "true");
            }
        });
        return exposedConfig;
    }

    private boolean isEnabled(DevServicesLoggingSplunkBuildTimeConfig config) {
        return config.enabled.orElse(false);
    }
}
