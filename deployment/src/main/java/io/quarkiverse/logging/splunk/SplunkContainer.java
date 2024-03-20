package io.quarkiverse.logging.splunk;

import static org.testcontainers.containers.wait.strategy.Wait.forLogMessage;

import java.time.Duration;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.devservices.common.ConfigureUtil;

public class SplunkContainer extends GenericContainer<SplunkContainer> {

    public static final int SPLUNK_UI_PORT = 8000;

    public static final int SPLUNK_HEC_PORT = 8088;

    public static final int SPLUNK_API_PORT = 8089;

    public static final String HEC_TOKEN = "local-dev-token";

    public static final String SPLUNK_PASSWORD = "admin123";

    public SplunkContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        withEnv("SPLUNK_START_ARGS", "--accept-license");
        withEnv("SPLUNK_PASSWORD", SPLUNK_PASSWORD);
        withEnv("SPLUNK_HEC_TOKEN", HEC_TOKEN);
        waitingFor(forLogMessage(".*Ansible playbook complete.*\\n", 1));
        withStartupTimeout(Duration.ofMinutes(2));
    }

    @Override
    protected void configure() {
        super.configure();
        withExposedPorts(SPLUNK_UI_PORT, SPLUNK_HEC_PORT, SPLUNK_API_PORT);
        ConfigureUtil.configureSharedNetwork(this, "splunk");
    }

    public String getSplunkUiUrl() {
        return "http://localhost:" + getMappedPort(SPLUNK_UI_PORT);
    }

    public String getSplunkHandlerUrl() {
        return getUrl(SPLUNK_HEC_PORT);
    }

    public String getSplunkApiUrl() {
        return getUrl(SPLUNK_API_PORT);
    }

    @NotNull
    private String getUrl(int port) {
        return "https://localhost:" + getMappedPort(port);
    }
}
