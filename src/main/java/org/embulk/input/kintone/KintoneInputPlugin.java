package org.embulk.input.kintone;

import com.google.common.annotations.VisibleForTesting;
import com.kintone.client.api.record.CreateCursorResponseBody;
import com.kintone.client.api.record.GetRecordsByCursorResponseBody;
import com.kintone.client.model.record.Record;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.Exec;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;
import org.embulk.util.config.ConfigMapper;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.config.TaskMapper;
import org.embulk.util.config.modules.TimestampModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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

        try {
            try (PageBuilder pageBuilder = getPageBuilder(schema, output)) {
                KintoneClient client = getKintoneClient();
                client.validateAuth(task);
                client.connect(task);

                CreateCursorResponseBody cursor = client.createCursor(task);
                GetRecordsByCursorResponseBody cursorResponse = new GetRecordsByCursorResponseBody(true, null);

                while (cursorResponse.isNext()) {
                    cursorResponse = client.getRecordsByCursor(cursor.getId());
                    for (Record record : cursorResponse.getRecords()) {
                        schema.visitColumns(new KintoneInputColumnVisitor(new KintoneAccessor(record), pageBuilder, task));
                        pageBuilder.addRecord();
                    }
                    pageBuilder.flush();
                }

                pageBuilder.finish();
            }
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
}
