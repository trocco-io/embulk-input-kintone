package org.embulk.input.kintone;

import com.google.common.annotations.VisibleForTesting;
import com.kintone.client.api.record.GetRecordsByCursorResponseBody;
import com.kintone.client.model.app.field.FieldProperty;
import com.kintone.client.model.record.FieldType;
import com.kintone.client.model.record.Record;
import com.kintone.client.model.record.TableRow;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.Exec;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;
import org.embulk.spi.Schema.Builder;
import org.embulk.spi.type.Type;
import org.embulk.spi.type.Types;
import org.embulk.util.config.ConfigMapper;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.config.TaskMapper;
import org.embulk.util.config.modules.TimestampModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class KintoneInputPlugin
        implements InputPlugin
{
    protected static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory.builder()
            .addDefaultModules()
            .addModule(new TimestampModule())
            .build();
    private final Logger logger = LoggerFactory.getLogger(KintoneInputPlugin.class);

    @Override
    public ConfigDiff transaction(ConfigSource config,
                                  InputPlugin.Control control)
    {
        final ConfigMapper configMapper = CONFIG_MAPPER_FACTORY.createConfigMapper();
        final PluginTask task = configMapper.map(config, PluginTask.class);

        Schema schema = task.getFields().toSchema();
        int taskCount = 1;  // number of run() method calls

        if (schema.isEmpty()) {
            schema = buildSchema(task);
        }

        return resume(task.toTaskSource(), schema, taskCount, control);
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource,
                             Schema schema, int taskCount,
                             InputPlugin.Control control)
    {
        control.run(taskSource, schema, taskCount);
        return CONFIG_MAPPER_FACTORY.newConfigDiff();
    }

    @Override
    public void cleanup(TaskSource taskSource,
                        Schema schema, int taskCount,
                        List<TaskReport> successTaskReports)
    {
    }

    @Override
    public TaskReport run(TaskSource taskSource,
                          Schema schema, int taskIndex,
                          PageOutput output)
    {
        final TaskMapper taskMapper = CONFIG_MAPPER_FACTORY.createTaskMapper();
        final PluginTask task = taskMapper.map(taskSource, PluginTask.class);

        try (PageBuilder pageBuilder = getPageBuilder(schema, output); KintoneClient client = getKintoneClient()) {
            client.validateAuth(task);
            client.connect(task);
            client.createCursor(task, schema);

            GetRecordsByCursorResponseBody cursorResponse = new GetRecordsByCursorResponseBody(true, null);

            List<String> subTableFieldCodes = null;
            if (task.getExpandSubtable()) {
                subTableFieldCodes = client.getFieldCodes(task, FieldType.SUBTABLE);
            }

            while (cursorResponse.isNext()) {
                cursorResponse = client.getRecordsByCursor();
                for (Record record : cursorResponse.getRecords()) {
                    List<Record> records;
                    if (task.getExpandSubtable()) {
                        records = expandSubtable(record, subTableFieldCodes);
                    }
                    else {
                        records = new ArrayList<>();
                        records.add(record);
                    }

                    for (Record expandedRecord : records) {
                        schema.visitColumns(new KintoneInputColumnVisitor(new KintoneAccessor(expandedRecord), pageBuilder, task));
                        pageBuilder.addRecord();
                    }
                }
                pageBuilder.flush();
            }

            pageBuilder.finish();
        }
        catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
        return CONFIG_MAPPER_FACTORY.newTaskReport();
    }

    @Override
    public ConfigDiff guess(ConfigSource config)
    {
        return CONFIG_MAPPER_FACTORY.newConfigDiff();
    }

    @VisibleForTesting
    protected PageBuilder getPageBuilder(final Schema schema, final PageOutput output)
    {
        return new PageBuilder(Exec.getBufferAllocator(), schema, output);
    }

    @VisibleForTesting
    protected KintoneClient getKintoneClient()
    {
        return new KintoneClient();
    }

    private List<Record> expandSubtable(final Record originalRecord, final List<String> subTableFieldCodes)
    {
        ArrayList<Record> records = new ArrayList<>();
        records.add(cloneRecord(originalRecord));
        for (String fieldCode : subTableFieldCodes) {
            List<TableRow> tableRows = originalRecord.getSubtableFieldValue(fieldCode);
            for (int idx = 0; idx < tableRows.size(); idx++) {
                if (records.size() < idx + 1) {
                    records.add(cloneRecord(originalRecord));
                }

                TableRow tableRow = tableRows.get(idx);
                Record currentRecord = records.get(idx);
                Set<String> tableFieldCodes = tableRow.getFieldCodes();
                for (String tableFieldCode : tableFieldCodes) {
                    currentRecord.putField(tableFieldCode, tableRow.getFieldValue(tableFieldCode));
                }
            }
        }
        return records;
    }

    private Record cloneRecord(final Record src)
    {
        Record dst = new Record(src.getId(), src.getRevision());
        for (String fieldCode : src.getFieldCodes(true)) {
            dst.putField(fieldCode, src.getFieldValue(fieldCode));
        }
        return dst;
    }

    private Schema buildSchema(final PluginTask task)
    {
        KintoneClient client = getKintoneClient();
        client.validateAuth(task);
        client.connect(task);

        Map<String, FieldProperty> fields = new TreeMap<>(client.getFields(task));
        Builder builder = Schema.builder();

        // built in schema
        builder.add("$id", Types.LONG);
        builder.add("$revision", Types.LONG);

        for (Map.Entry<String, FieldProperty> fieldEntry : fields.entrySet()) {
            final Type type = buildType(fieldEntry.getValue().getType());
            if (type != null) {
                builder.add(fieldEntry.getKey(), type);
            }
        }

        return builder.build();
    }

    private Type buildType(final FieldType fieldType)
    {
        switch(fieldType) {
            case __ID__:
            case __REVISION__:
            case RECORD_NUMBER:
                return Types.LONG;
            case CALC:
            case NUMBER:
                return Types.DOUBLE;
            case CREATED_TIME:
            case DATETIME:
            case UPDATED_TIME:
                return Types.TIMESTAMP;
            case CREATOR:
            case GROUP_SELECT:
            case MODIFIER:
            case ORGANIZATION_SELECT:
            case STATUS_ASSIGNEE:
            case SUBTABLE:
            case USER_SELECT:
                return Types.JSON;
            case CATEGORY:
            case CHECK_BOX:
            case DATE:
            case DROP_DOWN:
            case FILE:
            case LINK:
            case MULTI_LINE_TEXT:
            case MULTI_SELECT:
            case RADIO_BUTTON:
            case RICH_TEXT:
            case SINGLE_LINE_TEXT:
            case STATUS:
            case TIME:
                return Types.STRING;
            default:
                return null;
        }
    }
}
