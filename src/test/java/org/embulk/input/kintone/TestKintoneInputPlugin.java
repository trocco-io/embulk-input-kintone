package org.embulk.input.kintone;

import com.cybozu.kintone.client.model.app.form.FieldType;
import com.cybozu.kintone.client.model.record.GetRecordsResponse;
import com.cybozu.kintone.client.model.record.field.FieldValue;

import com.kintone.client.api.record.GetRecordsByCursorResponseBody;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.Schema;
import org.embulk.spi.TestPageBuilderReader.MockPageOutput;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.time.TimestampParser;
import org.embulk.spi.util.Pages;
import org.embulk.test.TestingEmbulk;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TestKintoneInputPlugin {
    private ConfigSource config;
    private static final String BASIC_RESOURCE_PATH = "org/embulk/input/kintone/";
    private KintoneInputPlugin kintoneInputPlugin;
    private KintoneClient kintoneClient;
    private MockPageOutput output = new MockPageOutput();
    private TimestampParser dateParser = TimestampParser.of("%Y-%m-%d", "UTC");
    private TimestampParser timestampParser = TimestampParser.of("%Y-%m-%dT%H:%M:%S%z", "UTC");

    private static ConfigSource loadYamlResource(TestingEmbulk embulk, String fileName) {
        return embulk.loadYamlResource(BASIC_RESOURCE_PATH + fileName);
    }

    @Before
    public void prepare(){
        kintoneInputPlugin = spy(new KintoneInputPlugin());
        kintoneClient = mock(KintoneClient.class);
        doReturn(kintoneClient).when(kintoneInputPlugin).getKintoneClient();
    }
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    @Rule
    public TestingEmbulk embulk = TestingEmbulk.builder()
            .registerPlugin(InputPlugin.class, "kintone", KintoneInputPlugin.class)
            .build();

    // Comment out for now
    // @Test
    public void simpleTest(){
        config = loadYamlResource(embulk, "base.yml");
        PluginTask task = config.loadConfig(PluginTask.class);
        Schema outputSchema =  task.getFields().toSchema();
//        GetRecordsResponse response = createSampleData(); // TODO: weida delete here
        GetRecordsByCursorResponseBody response = createSampleData();
        when(kintoneClient.getResponse(any(PluginTask.class))).thenReturn(response);

        ConfigDiff configDiff = kintoneInputPlugin.transaction(config, new Control());

        assertTrue(configDiff.isEmpty());

        List<Object[]> outputRecords = Pages.toObjects(outputSchema, output.pages);
        Object[] record1 = outputRecords.get(0);

        Timestamp date1 = dateParser.parse("2020-01-01");
        Timestamp timestamp1 = timestampParser.parse("2020-01-01T00:00:00Z");

        assertEquals(2, outputRecords.size());

        assertEquals("test single text", record1[0]);
        assertEquals(1L, record1[1]);
        assertEquals(1.111, record1[2]);
        assertEquals(date1, record1[3]);
        assertEquals(timestamp1, record1[4]);

        Timestamp date2 = dateParser.parse("2020-02-02");
        Timestamp timestamp2 = timestampParser.parse("2020-02-02T00:00:00Z");

        Object[] record2 = outputRecords.get(1);
        assertEquals("test single text2", record2[0]);
        assertEquals(2L, record2[1]);
        assertEquals(2.222, record2[2]);
        assertEquals(date2, record2[3]);
        assertEquals(timestamp2, record2[4]);
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

    private GetRecordsResponse createSampleData(){
        HashMap<String, FieldValue> record1 = new HashMap<>();
        HashMap<String, FieldValue> record2 = new HashMap<>();
        ArrayList<HashMap<String, FieldValue>> records = new ArrayList<>();
        GetRecordsResponse response = new GetRecordsResponse();

        record1 = TestHelper.addField(record1, "foo", FieldType.SINGLE_LINE_TEXT, "test single text");
        record1 = TestHelper.addField(record1, "bar", FieldType.NUMBER, 1);
        record1 = TestHelper.addField(record1, "baz", FieldType.NUMBER, 1.111);
        record1 = TestHelper.addField(record1, "date", FieldType.DATE, "2020-01-01");
        record1 = TestHelper.addField(record1, "datetime", FieldType.DATE, "2020-01-01T00:00:00Z");
        records.add(record1);

        record2 = TestHelper.addField(record2, "foo", FieldType.SINGLE_LINE_TEXT, "test single text2");
        record2 = TestHelper.addField(record2, "bar", FieldType.NUMBER, 2);
        record2 = TestHelper.addField(record2, "baz", FieldType.NUMBER, 2.222);
        record2 = TestHelper.addField(record2, "date", FieldType.DATE, "2020-02-02");
        record2 = TestHelper.addField(record2, "datetime", FieldType.DATE, "2020-02-02T00:00:00Z");
        records.add(record2);

        response.setRecords(records);
        response.setTotalCount(2);
        return response;
    }

    private class Control implements InputPlugin.Control
    {
        @Override
        public List<TaskReport> run(final TaskSource taskSource, final Schema schema, final int taskCount)
        {
            List<TaskReport> reports = IntStream.range(0, taskCount)
                    .mapToObj(i -> kintoneInputPlugin.run(taskSource, schema, i, output))
                    .collect(Collectors.toList());
            return reports;
        }
    }
}
