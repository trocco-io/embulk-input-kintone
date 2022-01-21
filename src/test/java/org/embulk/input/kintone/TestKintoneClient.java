package org.embulk.input.kintone;

import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.spi.InputPlugin;

import org.embulk.test.TestingEmbulk;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestKintoneClient {
    private ConfigSource config;
    private KintoneClient client = new KintoneClient();
    private static final String BASIC_RESOURCE_PATH = "org/embulk/input/kintone/";
    private static final String SUCCESS_MSG = "Exception should be thrown by this";

    private static ConfigSource loadYamlResource(TestingEmbulk embulk, String fileName) {
        return embulk.loadYamlResource(BASIC_RESOURCE_PATH + fileName);
    }

    @Rule
    public TestingEmbulk embulk = TestingEmbulk.builder()
            .registerPlugin(InputPlugin.class, "kintone", KintoneInputPlugin.class)
            .build();

    @Test
    public void checkClientWithUsernameAndPassword() {
        config = loadYamlResource(embulk, "base.yml");
        System.out.println("UUID weida"); // TODO :weida delete here
        System.out.println(config.toString());
        PluginTask task = config.loadConfig(PluginTask.class);
        Exception e = assertThrows(Exception.class, ()-> {
            client.validateAuth(task);
            throw new Exception(SUCCESS_MSG);
        });
        assertEquals(SUCCESS_MSG, e.getMessage());
    }

    @Test
    public void checkThrowErrorWithoutAuthInfo() {
        config = loadYamlResource(embulk, "base.yml");
        config.remove("username")
                .remove("password");
        PluginTask task = config.loadConfig(PluginTask.class);
        ConfigException e = assertThrows(ConfigException.class, () -> client.validateAuth(task));
        assertEquals("Username and password or token must be provided", e.getMessage());
    }

    @Test
    public void checkClientErrorLackingPassword() {
        config = loadYamlResource(embulk, "base.yml");
        config.remove("password");
        PluginTask task = config.loadConfig(PluginTask.class);
        ConfigException e = assertThrows(ConfigException.class, () -> client.validateAuth(task));
        assertEquals("Username and password or token must be provided", e.getMessage());
    }

    @Test
    public void checkClientErrorLackingUsername() {
        config = loadYamlResource(embulk, "base.yml");
        config.remove("username");
        PluginTask task = config.loadConfig(PluginTask.class);
        ConfigException e = assertThrows(ConfigException.class, () -> client.validateAuth(task));
        assertEquals("Username and password or token must be provided", e.getMessage());
    }

    @Test
    public void checkClientWithToken() {
        config = loadYamlResource(embulk, "base.yml");
        config.remove("username")
                .remove("password")
                .set("token", "token");
        PluginTask task = config.loadConfig(PluginTask.class);
        Exception e = assertThrows(Exception.class, ()-> {
            client.validateAuth(task);
            throw new Exception(SUCCESS_MSG);
        });
        assertEquals(SUCCESS_MSG, e.getMessage());
    }
}
