package org.embulk.input.kintone;


import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.Column;
import org.embulk.spi.Exec;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.Schema;
import org.embulk.spi.type.Types;
import org.embulk.spi.util.Pages;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class TestKintoneInputPlugin
{
    private final String kintoneDomain = "dev.cybozu.com";
    private ConfigSource config;
    private KintoneInputPlugin plugin;

    @Before
    public void createResources(){
       config = config();
       plugin = new KintoneInputPlugin();
    }

    @Test
    public void checkDefaultConfigValues)(){
        PluginTask task = config.loadConfig(PluginTask.class);
        assertEquals("{}", task.getFields());
    }


    private ConfigSource config(){
        return Exec.newConfigSource()
                .set("domain", kintoneDomain)
                .set("app_id", 1)
                .set("username", "username")
                .set("password", "password");
    }
}
