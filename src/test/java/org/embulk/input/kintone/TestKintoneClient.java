package org.embulk.input.kintone;

import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.spi.InputPlugin;
import org.embulk.test.TestingEmbulk;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TestKintoneClient
{
    private ConfigSource config;
    private final KintoneClient client = new KintoneClient();
    private static final String BASIC_RESOURCE_PATH = "org/embulk/input/kintone/";
    private static final String SUCCESS_MSG = "Exception should be thrown by this";
    private final org.embulk.util.config.ConfigMapper configMapper = KintoneInputPlugin.CONFIG_MAPPER_FACTORY.createConfigMapper();

    private static ConfigSource loadYamlResource(TestingEmbulk embulk)
    {
        return embulk.loadYamlResource(BASIC_RESOURCE_PATH + "base.yml");
    }

    @Rule
    public TestingEmbulk embulk = TestingEmbulk.builder()
            .registerPlugin(InputPlugin.class, "kintone", KintoneInputPlugin.class)
            .build();

    @Test
    public void checkClientWithUsernameAndPassword()
    {
        config = loadYamlResource(embulk);
        PluginTask task = configMapper.map(config, PluginTask.class);
        Exception e = assertThrows(Exception.class, ()-> {
            client.validateAuth(task);
            throw new Exception(SUCCESS_MSG);
        });
        assertEquals(SUCCESS_MSG, e.getMessage());
    }

    @Test
    public void checkThrowErrorWithoutAuthInfo()
    {
        config = loadYamlResource(embulk);
        config.remove("username")
                .remove("password");
        PluginTask task = configMapper.map(config, PluginTask.class);
        ConfigException e = assertThrows(ConfigException.class, () -> client.validateAuth(task));
        assertEquals("Username and password or token must be provided", e.getMessage());
    }

    @Test
    public void checkClientErrorLackingPassword()
    {
        config = loadYamlResource(embulk);
        config.remove("password");
        PluginTask task = configMapper.map(config, PluginTask.class);
        ConfigException e = assertThrows(ConfigException.class, () -> client.validateAuth(task));
        assertEquals("Username and password or token must be provided", e.getMessage());
    }

    @Test
    public void checkClientErrorLackingUsername()
    {
        config = loadYamlResource(embulk);
        config.remove("username");
        PluginTask task = configMapper.map(config, PluginTask.class);
        ConfigException e = assertThrows(ConfigException.class, () -> client.validateAuth(task));
        assertEquals("Username and password or token must be provided", e.getMessage());
    }

    @Test
    public void checkClientWithToken()
    {
        config = loadYamlResource(embulk);
        config.remove("username")
                .remove("password")
                .set("token", "token");
        PluginTask task = configMapper.map(config, PluginTask.class);
        Exception e = assertThrows(Exception.class, ()-> {
            client.validateAuth(task);
            throw new Exception(SUCCESS_MSG);
        });
        assertEquals(SUCCESS_MSG, e.getMessage());
    }
}
