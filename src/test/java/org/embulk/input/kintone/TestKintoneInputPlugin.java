package org.embulk.input.kintone;

import com.cybozu.kintone.client.connection.Connection;
import com.cybozu.kintone.client.exception.KintoneAPIException;
import com.cybozu.kintone.client.model.app.form.FieldType;
import com.cybozu.kintone.client.model.record.GetRecordsResponse;
import com.cybozu.kintone.client.model.record.field.FieldValue;
import com.google.gson.JsonElement;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.Schema;
import org.embulk.spi.TestPageBuilderReader.MockPageOutput;

import org.embulk.test.TestingEmbulk;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class TestKintoneInputPlugin {
    private ConfigSource config;
    private MockPageOutput output;
    private KintoneInputPlugin plugin;
    private KintoneClient kintoneClient;
    private static final String BASIC_RESOURCE_PATH = "org/embulk/input/kintone/";
    private PageBuilder pageBuilder;

    @Rule
    public TestingEmbulk embulk = TestingEmbulk.builder()
            .registerPlugin(InputPlugin.class, "kintone", KintoneInputPlugin.class)
            .build();

    private static ConfigSource loadYamlResource(TestingEmbulk embulk, String fileName) {
        return embulk.loadYamlResource(BASIC_RESOURCE_PATH + fileName);
    }

    @Test
    public void simpleTest() throws IOException, KintoneAPIException
    {
        config = loadYamlResource(embulk, "base.yml");
        plugin = Mockito.spy(new KintoneInputPlugin());
        kintoneClient = Mockito.spy(new KintoneClient());
        output = new MockPageOutput();
        pageBuilder = Mockito.mock(PageBuilder.class);

        HashMap<String, FieldValue> testRecord = new HashMap<>();
        ArrayList<HashMap<String, FieldValue>> records = new ArrayList<>();
        GetRecordsResponse response = new GetRecordsResponse();

        testRecord = addField(testRecord, "foo", FieldType.SINGLE_LINE_TEXT, "test single text");
        testRecord = addField(testRecord, "bar", FieldType.NUMBER, 1);
        testRecord = addField(testRecord, "baz", FieldType.NUMBER, 2.222);
        records.add(testRecord);
        response.setRecords(records);
        response.setTotalCount(1);
        System.out.println(kintoneClient);

        when(plugin.getKintoneClient()).thenReturn(kintoneClient);
        doReturn(response).when(kintoneClient).getResponse(Mockito.any(PluginTask.class));
        doReturn(pageBuilder).when(plugin).getPageBuilder(Mockito.any(), Mockito.any());

        // JsonElement records = TestHelpers.jsonObj2JsonElement(TestHelpers.getJsonFromFile("org/embulk/input/kintone/records.json"));

        // Mockito.doReturn(records).when(Mockito.any(Connection.class)).request(Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
        plugin.transaction(config, new Control());
        System.err.println(output.pages.get(0));

        // https://github.com/kintone/kintone-java-sdk/blob/master/src/main/java/com/cybozu/kintone/client/module/record/Record.java#L123
        // request mock
        // package com.cybozu.kintone.client.connection;
        // Connection.request return JSONElement

        // ConfigDiff configDiff = runner.transaction(config, new Control(runner, output));

        // assertEquals(EMBULK_S3_TEST_PATH_PREFIX + "/sample_01.csv", configDiff.get(String.class, "last_path"));
        // assertRecords(config, output);
    }

    private HashMap<String, FieldValue> addField(HashMap<String, FieldValue> record, String code, FieldType type,
                                                 Object value) {
        FieldValue newField = new FieldValue();
        newField.setType(type);
        newField.setValue(value);
        record.put(code, newField);
        return record;
    }


    private class Control
            implements InputPlugin.Control
    {
        @Override
        public List<TaskReport> run(TaskSource taskSource, Schema schema, int taskCount)
        {
            List<TaskReport> reports = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                reports.add(plugin.run(taskSource, schema, i, output));
            }
            return reports;
        }
    }


    @Test
    public void checkDefaultConfigValues() {
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
