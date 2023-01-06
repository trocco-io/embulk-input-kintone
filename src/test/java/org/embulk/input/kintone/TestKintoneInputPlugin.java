package org.embulk.input.kintone;

import com.kintone.client.api.record.GetRecordsByCursorResponseBody;
import com.kintone.client.model.record.DateFieldValue;
import com.kintone.client.model.record.DateTimeFieldValue;
import com.kintone.client.model.record.NumberFieldValue;
import com.kintone.client.model.record.Record;
import com.kintone.client.model.record.SingleLineTextFieldValue;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.Exec;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.Page;
import org.embulk.spi.PageReader;
import org.embulk.spi.Schema;
import org.embulk.spi.TestPageBuilderReader.MockPageOutput;
import org.embulk.test.TestingEmbulk;
import org.embulk.util.timestamp.TimestampFormatter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class TestKintoneInputPlugin
{
    private ConfigSource config;
    private static final String BASIC_RESOURCE_PATH = "org/embulk/input/kintone/";
    private KintoneInputPlugin kintoneInputPlugin;
    private KintoneClient kintoneClient;
    private final MockPageOutput output = new MockPageOutput();
    private final org.embulk.util.config.ConfigMapper configMapper = KintoneInputPlugin.CONFIG_MAPPER_FACTORY.createConfigMapper();
    TimestampFormatter dateParser = TimestampFormatter.builder("ruby:%Y-%m-%d").setDefaultZoneOffset(ZoneOffset.UTC).build();
    TimestampFormatter timestampParser = TimestampFormatter.builder("ruby:%Y-%m-%dT%H:%M:%S%z").setDefaultZoneOffset(ZoneOffset.UTC).build();

    private static ConfigSource loadYamlResource(TestingEmbulk embulk)
    {
        return embulk.loadYamlResource(BASIC_RESOURCE_PATH + "base.yml");
    }

    @Before
    public void prepare()
    {
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
    @Test
    public void simpleTest()
    {
        config = loadYamlResource(embulk);
        PluginTask task = configMapper.map(config, PluginTask.class);
        Schema outputSchema =  task.getFields().toSchema();
        GetRecordsByCursorResponseBody response = createSampleData();
        when(kintoneClient.getResponse(any(PluginTask.class))).thenReturn(response);

        ConfigDiff configDiff = kintoneInputPlugin.transaction(config, new Control());

        assertTrue(configDiff.isEmpty());

        List<PageReader> outputRecords = output.pages.stream().map((page) -> getPageReader(outputSchema, page)).collect(Collectors.toList());
        PageReader reader1 = outputRecords.get(0);

        Instant date1 = dateParser.parse("2020-01-01");
        Instant timestamp1 = timestampParser.parse("2020-01-01T00:00:00Z");

        assertEquals(2, outputRecords.size());

        assertEquals("test single text", reader1.getString(0));
        assertEquals(1L, reader1.getLong(1));
        assertEquals(1.111, reader1.getDouble(2), 0);
        assertEquals(date1, reader1.getTimestampInstant(3));
        assertEquals(timestamp1, reader1.getTimestampInstant(4));

        Instant date2 = dateParser.parse("2020-02-02");
        Instant timestamp2 = timestampParser.parse("2020-02-02T00:00:00Z");

        PageReader reader2 = outputRecords.get(1);
        assertEquals("test single text2", reader2.getString(0));
        assertEquals(2L, reader2.getLong(1));
        assertEquals(2.222, reader2.getDouble(2), 0);
        assertEquals(date2, reader2.getTimestampInstant(3));
        assertEquals(timestamp2, reader2.getTimestampInstant(4));
    }

    @Test
    public void checkDefaultConfigValues()
    {
        config = loadYamlResource(embulk);
        PluginTask task = configMapper.map(config, PluginTask.class);
        assertEquals("dev.cybozu.com", task.getDomain());
        assertEquals(1, task.getAppId());
        assertEquals("username", task.getUsername().orElse(null));
        assertEquals("password", task.getPassword().orElse(null));
        assertFalse(task.getToken().isPresent());
        assertFalse(task.getGuestSpaceId().isPresent());
        assertFalse(task.getBasicAuthUsername().isPresent());
        assertFalse(task.getBasicAuthPassword().isPresent());
        assertFalse(task.getQuery().isPresent());
        assertNotNull(task.getFields());
    }

    private GetRecordsByCursorResponseBody createSampleData()
    {
        Record record1 = new Record();
        Record record2 = new Record();
        ArrayList<Record> records = new ArrayList<>(2);

        record1.putField("foo",  new SingleLineTextFieldValue("test single text"));
        record1.putField("bar",  new NumberFieldValue(1L));
        record1.putField("baz",  new NumberFieldValue(1L));
        record1.putField("date",  new DateFieldValue(LocalDate.of(2020, 1, 1)));
        record1.putField("datetime",  new DateTimeFieldValue(ZonedDateTime.parse("2020-01-01T00:00:00Z")));
        records.add(record1);

        record2.putField("foo",  new SingleLineTextFieldValue("test single text2"));
        record2.putField("bar",  new NumberFieldValue(2L));
        record2.putField("baz",  new NumberFieldValue(2L));
        record2.putField("date",  new DateFieldValue(LocalDate.of(2020, 2, 2)));
        record2.putField("datetime",  new DateTimeFieldValue(ZonedDateTime.parse("2020-02-02T00:00:00Z")));
        records.add(record2);

        return new GetRecordsByCursorResponseBody(false, records);
    }

    private class Control implements InputPlugin.Control
    {
        @Override
        public List<TaskReport> run(final TaskSource taskSource, final Schema schema, final int taskCount)
        {
            return IntStream.range(0, taskCount)
                    .mapToObj(i -> kintoneInputPlugin.run(taskSource, schema, i, output))
                    .collect(Collectors.toList());
        }
    }

    private PageReader getPageReader(final Schema schema, final Page page)
    {
        final PageReader reader = Exec.getPageReader(schema);
        reader.setPage(page);
        return reader;
    }
}
