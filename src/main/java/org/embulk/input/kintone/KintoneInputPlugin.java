package org.embulk.input.kintone;

import com.cybozu.kintone.client.model.record.GetRecordsResponse;
import com.cybozu.kintone.client.model.record.field.FieldValue;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.spi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

public class KintoneInputPlugin
        implements InputPlugin {
    private final Logger logger = LoggerFactory.getLogger(KintoneInputPlugin.class);

    @Override
    public ConfigDiff transaction(ConfigSource config,
                                  InputPlugin.Control control) {
        PluginTask task = config.loadConfig(PluginTask.class);

        Schema schema = task.getFields().toSchema();
        int taskCount = 1;  // number of run() method calls

        return resume(task.dump(), schema, taskCount, control);
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource,
                             Schema schema, int taskCount,
                             InputPlugin.Control control) {
        control.run(taskSource, schema, taskCount);
        return Exec.newConfigDiff();
    }

    @Override
    public void cleanup(TaskSource taskSource,
                        Schema schema, int taskCount,
                        List<TaskReport> successTaskReports) {
    }

    @Override
    public TaskReport run(TaskSource taskSource,
                          Schema schema, int taskIndex,
                          PageOutput output) {
        PluginTask task = taskSource.loadTask(PluginTask.class);

        try {
            try (PageBuilder pageBuilder = getPageBuilder(schema, output)) {
                KintoneClient client = getKintoneClient();
                client.validateAuth(task);
                client.connect(task);
                GetRecordsResponse response = client.getResponse(task);
                for (HashMap<String, FieldValue> record : response.getRecords()) {
                    schema.visitColumns(new KintoneInputColumnVisitor(new KintoneAccessor(record), pageBuilder, task));
                    pageBuilder.addRecord();
                }
                pageBuilder.finish();
                client.deleteCursor();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw e;
        }
        return Exec.newTaskReport();
    }

    @Override
    public ConfigDiff guess(ConfigSource config) {
        return Exec.newConfigDiff();
    }

    @VisibleForTesting
    protected PageBuilder getPageBuilder(final Schema schema, final PageOutput output)
    {
        return new PageBuilder(Exec.getBufferAllocator(), schema, output);
    }

    @VisibleForTesting
    protected KintoneClient getKintoneClient(){
        return new KintoneClient();
    }

}
