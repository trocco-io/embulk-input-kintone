package org.embulk.input.kintone;

import com.kintone.client.api.record.GetRecordsByCursorResponseBody;
import com.kintone.client.model.record.*;

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

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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

    private GetRecordsByCursorResponseBody createSampleData(){
        Record record1 = new Record();
        Record record2 = new Record();
        ArrayList<Record> records = new ArrayList<>(2);

        record1.putField("foo",  new SingleLineTextFieldValue("test single text"));
        record1.putField("bar",  new NumberFieldValue(Long.valueOf(1)));
        record1.putField("baz",  new NumberFieldValue(Long.valueOf(1)));
        record1.putField("date",  new DateFieldValue(LocalDate.of(2020,1,1)));
        record1.putField("datetime",  new DateTimeFieldValue(ZonedDateTime.parse("2020-01-01T00:00:00Z")));
        records.add(record1);

        record2.putField("foo",  new SingleLineTextFieldValue("test single text2"));
        record2.putField("bar",  new NumberFieldValue(Long.valueOf(2)));
        record2.putField("baz",  new NumberFieldValue(Long.valueOf(2)));
        record2.putField("date",  new DateFieldValue(LocalDate.of(2020,2,2)));
        record2.putField("datetime",  new DateTimeFieldValue(ZonedDateTime.parse("2020-02-02T00:00:00Z")));
        records.add(record2);

        GetRecordsByCursorResponseBody response = new GetRecordsByCursorResponseBody(false, records);
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
