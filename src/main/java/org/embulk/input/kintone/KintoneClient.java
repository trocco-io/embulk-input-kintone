package org.embulk.input.kintone;

import com.google.common.annotations.VisibleForTesting;
import com.kintone.client.AppClient;
import com.kintone.client.KintoneClientBuilder;
import com.kintone.client.RecordClient;
import com.kintone.client.api.record.CreateCursorRequest;
import com.kintone.client.api.record.CreateCursorResponseBody;
import com.kintone.client.api.record.GetRecordsByCursorResponseBody;
import com.kintone.client.exception.KintoneApiRuntimeException;
import com.kintone.client.model.app.field.FieldProperty;
import com.kintone.client.model.app.field.SubtableFieldProperty;
import com.kintone.client.model.record.FieldType;
import org.embulk.config.ConfigException;
import org.embulk.spi.Column;
import org.embulk.spi.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KintoneClient
{
    private final Logger logger = LoggerFactory.getLogger(KintoneClient.class);
    private static final int FETCH_SIZE = 500;
    private RecordClient recordClient;
    private AppClient appClient;

    public KintoneClient() throws ConfigException
    {
    }

    @VisibleForTesting
    protected KintoneClient(AppClient appClient)
    {
        this.appClient = appClient;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public void validateAuth(final PluginTask task) throws ConfigException
    {
        if (task.getUsername().isPresent() && task.getPassword().isPresent()) {
            // NOP
        }
        else if (task.getToken().isPresent()) {
            // NOP
        }
        else {
            throw new ConfigException("Username and password or token must be provided");
        }
    }

    public void connect(final PluginTask task)
    {
        KintoneClientBuilder builder = KintoneClientBuilder.create(String.format("https://%s", task.getDomain()));
        if (task.getUsername().isPresent() && task.getPassword().isPresent()) {
            builder.authByPassword(task.getUsername().get(), task.getPassword().get());
        }
        else if (task.getToken().isPresent()) {
            builder.authByApiToken(task.getToken().get());
        }

        if (task.getBasicAuthUsername().isPresent() && task.getBasicAuthPassword().isPresent()) {
            builder.withBasicAuth(task.getBasicAuthUsername().get(), task.getBasicAuthPassword().get());
        }

        if (task.getGuestSpaceId().isPresent()) {
            builder.setGuestSpaceId(task.getGuestSpaceId().orElse(-1));
        }

        com.kintone.client.KintoneClient client = builder.build();
        this.recordClient = client.record();
        this.appClient = client.app();
    }

    public GetRecordsByCursorResponseBody getResponse(final PluginTask task, final Schema schema)
    {
        CreateCursorResponseBody cursor = this.createCursor(task, schema);
        try {
            return this.recordClient.getRecordsByCursor(cursor.getId());
        }
        catch (KintoneApiRuntimeException e) {
            this.logger.error(e.toString());
            this.deleteCursor(cursor.getId());
            throw new RuntimeException(e);
        }
    }

    public GetRecordsByCursorResponseBody getRecordsByCursor(String cursor)
    {
        try {
            return this.recordClient.getRecordsByCursor(cursor);
        }
        catch (KintoneApiRuntimeException e) {
            this.logger.error(e.toString());
            this.deleteCursor(cursor);
            throw new RuntimeException(e);
        }
    }

    public CreateCursorResponseBody createCursor(final PluginTask task, final Schema schema)
    {
        ArrayList<String> fields = new ArrayList<>();
        for (Column c : schema.getColumns()) {
            fields.add(c.getName());
        }
        if (task.getExpandSubtable()) {
            List<String> subTableFieldCodes = getFieldCodes(task, FieldType.SUBTABLE);
            fields.addAll(subTableFieldCodes);
        }
        CreateCursorRequest request = new CreateCursorRequest();
        request.setApp((long) task.getAppId());
        request.setFields(fields);
        request.setQuery(task.getQuery().orElse(""));
        request.setSize((long) FETCH_SIZE);
        try {
            return this.recordClient.createCursor(request);
        }
        catch (KintoneApiRuntimeException e) {
            this.logger.error(e.toString());
            throw new RuntimeException(e);
        }
    }

    public void deleteCursor(String cursor)
    {
        try {
            this.recordClient.deleteCursor(cursor);
        }
        catch (KintoneApiRuntimeException e) {
            this.logger.error(e.toString());
        }
    }

    public Map<String, FieldProperty> getFields(final PluginTask task)
    {
        Map<String, FieldProperty> fields = this.appClient.getFormFields(task.getAppId());
        if (task.getExpandSubtable()) {
            Map<String, FieldProperty> subtableFields = new HashMap<>();
            List<String> subtableFieldCodes = new ArrayList<>();
            for (Map.Entry<String, FieldProperty> fieldEntry : fields.entrySet()) {
                if (fieldEntry.getValue().getType() == FieldType.SUBTABLE) {
                    subtableFields.putAll(((SubtableFieldProperty) fieldEntry.getValue()).getFields());
                    subtableFieldCodes.add(fieldEntry.getKey());
                }
            }
            for (String subtableFieldCode : subtableFieldCodes) {
                fields.remove(subtableFieldCode);
            }
            fields.putAll(subtableFields);
        }

        return fields;
    }

    public List<String> getFieldCodes(final PluginTask task, FieldType fieldType)
    {
        ArrayList<String> fieldCodes = new ArrayList<>();
        Map<String, FieldProperty> fields = this.appClient.getFormFields(task.getAppId());
        for (Map.Entry<String, FieldProperty> entry : fields.entrySet()) {
            if (entry.getValue().getType() == fieldType) {
                fieldCodes.add(entry.getKey());
            }
        }
        return fieldCodes;
    }
}
