package org.embulk.input.kintone;

import java.util.HashMap;
import java.util.List;

import org.embulk.config.*;
import org.embulk.spi.Exec;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.Schema;
import org.embulk.spi.PageOutput;

import com.cybozu.kintone.client.model.record.field.FieldValue;
import com.cybozu.kintone.client.model.record.GetRecordsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            try (PageBuilder pageBuilder = new PageBuilder(Exec.getBufferAllocator(), schema, output)) {
                KintoneClient client = new KintoneClient(task);
                // TODO: interface should accept query?
                GetRecordsResponse response = client.getResponse();
                for (HashMap<String, FieldValue> record : response.getRecords()) {
                    schema.visitColumns(new KintoneInputColumnVisitor(new KintoneAccessor(record), pageBuilder, task));
                    pageBuilder.addRecord();
                }
                pageBuilder.finish();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println(e.fillInStackTrace());
        }
        return Exec.newTaskReport();
    }

    @Override
    public ConfigDiff guess(ConfigSource config) {
        return Exec.newConfigDiff();
    }
}
