package org.embulk.input.kintone;

import org.embulk.config.ConfigSource;
import org.embulk.spi.InputPlugin;

import org.embulk.test.TestingEmbulk;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestKintoneInputPlugin
{
    private ConfigSource config;
    private static final String BASIC_RESOURCE_PATH = "org/embulk/input/kintone/";

    private static ConfigSource loadYamlResource(TestingEmbulk embulk, String fileName)
    {
        return embulk.loadYamlResource(BASIC_RESOURCE_PATH + fileName);
    }

    @Rule
    public TestingEmbulk embulk = TestingEmbulk.builder()
            .registerPlugin(InputPlugin.class, "kintone", KintoneInputPlugin.class)
            .build();

    @Test
    public void checkDefaultConfigValues(){
        config = loadYamlResource(embulk, "base.yml");
        PluginTask task = config.loadConfig(PluginTask.class);
        assertEquals("dev.cybozu.com", task.getDomain());
        assertEquals(1, task.getAppId());
        assertEquals("username", task.getUsername().get());
        assertEquals("password", task.getPassword().get());
        assertFalse(task.getToken().isPresent());
        assertFalse(task.getGuestSpaceId().isPresent());
        assertFalse(task.getBasicAuthUsername().isPresent());
        assertFalse(task.getBasicAuthPassword().isPresent());
        assertFalse(task.getQuery().isPresent());
        assertNotNull(task.getFields());
    }
}
