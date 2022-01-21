package org.embulk.input.kintone;

import com.kintone.client.KintoneClientBuilder;
import com.kintone.client.RecordClient;
import com.kintone.client.api.record.CreateCursorRequest;
import com.kintone.client.api.record.CreateCursorResponseBody;
import com.kintone.client.api.record.GetRecordsByCursorResponseBody;
import com.kintone.client.exception.KintoneApiRuntimeException;
import org.embulk.config.ConfigException;
import org.embulk.spi.ColumnConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class KintoneClient {
    private final Logger logger = LoggerFactory.getLogger(KintoneClient.class);
    private static final int FETCH_SIZE = 500;
    private com.kintone.client.KintoneClient client;
    private RecordClient recordClient;

    public KintoneClient() throws ConfigException { }

    public void validateAuth(final PluginTask task) throws ConfigException {
        if (task.getUsername().isPresent() && task.getPassword().isPresent()) {
            return;
        } else if (task.getToken().isPresent()) {
            return;
        } else {
            throw new ConfigException("Username and password or token must be provided");
        }
    }

    public void connect(final PluginTask task) {
        System.out.println(String.format("https://%s", task.getDomain()));
        KintoneClientBuilder builder = KintoneClientBuilder.create(String.format("https://%s", task.getDomain()));
        if (task.getUsername().isPresent() && task.getPassword().isPresent()) {
            builder.authByPassword(task.getUsername().get(), task.getPassword().get());
        } else if (task.getToken().isPresent()) {
            builder.authByApiToken(task.getToken().get());
        }

        if (task.getBasicAuthUsername().isPresent() && task.getBasicAuthPassword().isPresent()) {
            builder.withBasicAuth(task.getBasicAuthUsername().get(), task.getBasicAuthPassword().get());
        }

        if (task.getGuestSpaceId().isPresent()) {
            builder.setGuestSpaceId(task.getGuestSpaceId().or(-1));
        }

        this.client = builder.build();
        this.recordClient = client.record();
    }


    public GetRecordsByCursorResponseBody getResponse(final PluginTask task) {
        CreateCursorResponseBody cursor = this.createCursor(task);
        try {
            return this.recordClient.getRecordsByCursor(cursor.getId());
        }catch (KintoneApiRuntimeException e){
            this.deleteCursor(cursor.getId());
            throw new RuntimeException(e);
        }
    }

    public GetRecordsByCursorResponseBody getRecordsByCursor(String cursor){
        try {
            return this.recordClient.getRecordsByCursor(cursor);
        }catch (KintoneApiRuntimeException e){
            this.deleteCursor(cursor);
            throw new RuntimeException(e);
        }
    }

    public CreateCursorResponseBody createCursor(final PluginTask task){
        ArrayList<String> fields = new ArrayList<>();
        for (ColumnConfig c : task.getFields().getColumns()
        ) {
            fields.add(c.getName());
        }
        CreateCursorRequest request = new CreateCursorRequest();
        request.setApp(Long.valueOf(task.getAppId()));
        request.setFields(fields);
        request.setQuery(task.getQuery().or(""));
        request.setSize(Long.valueOf(FETCH_SIZE)); // there is no other way to set up property of size hence set it via CreateCursorRequest
        try{
            return this.recordClient.createCursor(request);
        }catch (KintoneApiRuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteCursor(String cursor) {
        try {
            this.recordClient.deleteCursor(cursor);
        }catch (KintoneApiRuntimeException e){
            this.logger.error(e.toString());
        }
    }
}
