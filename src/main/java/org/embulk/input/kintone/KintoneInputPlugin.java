package org.embulk.input.kintone;

import com.kintone.client.api.record.CreateCursorResponseBody;
import com.kintone.client.api.record.GetRecordsByCursorResponseBody;
//import com.kintone.kintone.client.model.cursor.CreateRecordCursorResponse;
//import com.kintone.kintone.client.model.cursor.GetRecordCursorResponse;
//import com.kintone.kintone.client.model.record.field.FieldValue; TODO: check here
import com.kintone.client.model.record.FieldValue;
import com.google.common.annotations.VisibleForTesting;
import com.kintone.client.model.record.Record;
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
                KintoneClient client = getKintoneClient(task);
                client.validateAuth(task);
                client.connect(task);

//                CreateRecordCursorResponse cursor = client.createCursor(task);
                CreateCursorResponseBody cursor = client.createCursor(task);
                GetRecordsByCursorResponseBody cursorResponse = new GetRecordsByCursorResponseBody(true, null);
//                cursorResponse.setNext(true);

//                while (cursorResponse.getNext()) {
                while (cursorResponse.isNext()) {
                    cursorResponse = client.getRecordsByCursor(cursor.getId());
//                    for (Record record : cursorResponse.getRecords()) {
//                        schema.visitColumns(new KintoneInputColumnVisitor(record, pageBuilder, task));
//                        pageBuilder.addRecord();
//                    }
                    for (Record record : cursorResponse.getRecords()) {
                        schema.visitColumns(new KintoneInputColumnVisitor(new KintoneAccessor(record), pageBuilder, task));
                        pageBuilder.addRecord();
                    }
                    pageBuilder.flush();
                }

                pageBuilder.finish();
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
    protected KintoneClient getKintoneClient(final PluginTask task){
        return new KintoneClient(task);
    }

}
