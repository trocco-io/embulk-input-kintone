package org.embulk.input.kintone;

import com.google.gson.JsonElement;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.time.Timestamp;
import org.embulk.util.config.units.ColumnConfig;
import org.embulk.util.json.JsonParser;
import org.embulk.util.timestamp.TimestampFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class KintoneInputColumnVisitor implements ColumnVisitor
{
    private static final String DEFAULT_TIMESTAMP_PATTERN = "%Y-%m-%dT%H:%M:%S%z";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final PageBuilder pageBuilder;
    private final PluginTask pluginTask;
    private final KintoneAccessor accessor;

    public KintoneInputColumnVisitor(final KintoneAccessor accessor, final PageBuilder pageBuilder, final PluginTask pluginTask)
    {
        this.accessor = accessor;
        this.pageBuilder = pageBuilder;
        this.pluginTask = pluginTask;
    }

    @Override
    public void stringColumn(Column column)
    {
        try {
            String data = accessor.get(column.getName());
            if (Objects.isNull(data)) {
                pageBuilder.setNull(column);
            }
            else {
                pageBuilder.setString(column, data);
            }
        }
        catch (Exception e) {
            logger.warn(String.format("Invalid string column: %s", column.getName()), e);
            pageBuilder.setNull(column);
        }
    }

    @Override
    public void booleanColumn(Column column)
    {
        try {
            String data = accessor.get(column.getName());
            pageBuilder.setBoolean(column, Boolean.parseBoolean(data));
        }
        catch (Exception e) {
            logger.warn(String.format("Invalid boolean column: %s", column.getName()), e);
            pageBuilder.setNull(column);
        }
    }

    @Override
    public void longColumn(Column column)
    {
        try {
            String data = accessor.get(column.getName());
            pageBuilder.setLong(column, Long.parseLong(data));
        }
        catch (Exception e) {
            logger.warn(String.format("Invalid long column: %s", column.getName()), e);
            pageBuilder.setNull(column);
        }
    }

    @Override
    public void doubleColumn(Column column)
    {
        try {
            String data = accessor.get(column.getName());
            pageBuilder.setDouble(column, Double.parseDouble(data));
        }
        catch (Exception e) {
            logger.warn(String.format("Invalid double column: %s", column.getName()), e);
            pageBuilder.setNull(column);
        }
    }

    @Override
    public void timestampColumn(Column column)
    {
        try {
            List<ColumnConfig> columnConfigs = pluginTask.getFields().getColumns();
            String pattern = DEFAULT_TIMESTAMP_PATTERN;
            for (ColumnConfig config : columnConfigs) {
                if (config.getName().equals(column.getName())
                        && config.getOption() != null
                        && config.getFormat() != null) {
                    pattern = config.getFormat();
                    break;
                }
            }
            final TimestampFormatter formatter = TimestampFormatter.builder("ruby:" + pattern).build();
            Instant instant = formatter.parse(accessor.get(column.getName()));
            pageBuilder.setTimestamp(column, Timestamp.ofInstant(instant));
        }
        catch (Exception e) {
            logger.warn(String.format("Invalid timestamp column: %s", column.getName()), e);
            pageBuilder.setNull(column);
        }
    }

    @Override
    public void jsonColumn(Column column)
    {
        try {
            JsonElement data = com.google.gson.JsonParser.parseString(accessor.get(column.getName()));
            if (data.isJsonNull() || data.isJsonPrimitive()) {
                pageBuilder.setNull(column);
            }
            else {
                pageBuilder.setJson(column, new JsonParser().parse(data.toString()));
            }
        }
        catch (Exception e) {
            logger.warn(String.format("Invalid json column: %s", column.getName()), e);
            pageBuilder.setNull(column);
        }
    }
}
