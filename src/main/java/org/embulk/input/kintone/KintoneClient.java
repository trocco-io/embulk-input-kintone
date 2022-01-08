package org.embulk.input.kintone;

//import com.cybozu.kintone.client.authentication.Auth;
//import com.cybozu.kintone.client.connection.Connection;
//import com.cybozu.kintone.client.exception.KintoneAPIException;
//import com.cybozu.kintone.client.model.cursor.CreateRecordCursorResponse;
//import com.cybozu.kintone.client.model.cursor.GetRecordCursorResponse;
//import com.cybozu.kintone.client.model.record.GetRecordsResponse;
//import com.cybozu.kintone.client.module.recordCursor.RecordCursor;
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
//    private Auth kintoneAuth; // TODO: weida delete here
    private com.kintone.client.KintoneClient client;
//    private RecordCursor kintoneRecordManager;  // TODO: weida delete here
    private RecordClient recordClient;
//    private Connection con; // TODO: weida delete here

    public KintoneClient(final PluginTask task) throws ConfigException { }

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
//            this.kintoneAuth.setPasswordAuth(task.getUsername().get(), task.getPassword().get()); // TODO: weida delete here
            builder.authByPassword(task.getUsername().get(), task.getPassword().get());
        } else if (task.getToken().isPresent()) {
//            this.kintoneAuth.setApiToken(task.getToken().get()); // TODO: weida delete here
            builder.authByApiToken(task.getToken().get());
        }

        if (task.getBasicAuthUsername().isPresent() && task.getBasicAuthPassword().isPresent()) {
//            this.kintoneAuth.setBasicAuth(task.getBasicAuthUsername().get(), // TODO: weida delete here
//                    task.getBasicAuthPassword().get());
            builder.withBasicAuth(task.getBasicAuthUsername().get(), task.getBasicAuthPassword().get());
        }

        if (task.getGuestSpaceId().isPresent()) {
            builder.setGuestSpaceId(task.getGuestSpaceId().or(-1));
        }

        // TODO: weida delete here
//        if (task.getGuestSpaceId().isPresent()) {
//            this.con = new Connection(task.getDomain(), this.kintoneAuth, task.getGuestSpaceId().or(-1));
//        } else {
//            this.con = new Connection(task.getDomain(), this.kintoneAuth);
//        }
        this.client = builder.build();
        this.recordClient = client.record();
//        this.kintoneRecordManager = new RecordCursor(con);
    }


//    public GetRecordsResponse getResponse(final PluginTask task) {
    public GetRecordsByCursorResponseBody getResponse(final PluginTask task) {
        CreateCursorResponseBody cursor = this.createCursor(task);
        try {
//            return this.kintoneRecordManager.getAllRecords(cursor.getId());
            return this.recordClient.getRecordsByCursor(cursor.getId());
        }catch (KintoneApiRuntimeException e){
            this.deleteCursor(cursor.getId());
            throw new RuntimeException(e);
        }
    }

//    public GetRecordCursorResponse getRecordsByCursor(CreateRecordCursorResponse cursor){
    public GetRecordsByCursorResponseBody getRecordsByCursor(String cursor){
        try {
//            return this.kintoneRecordManager.getRecords(cursor.getId());
            return this.recordClient.getRecordsByCursor(cursor);
        }catch (KintoneApiRuntimeException e){
            this.deleteCursor(cursor);
            throw new RuntimeException(e);
        }
    }

//    public CreateRecordCursorResponse createCursor(final PluginTask task){
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
//            return this.kintoneRecordManager.createCursor(task.getAppId(),  fields, task.getQuery().or(""), FETCH_SIZE);
//        }catch (KintoneAPIException e) { // TODO: weida delete here
        }catch (KintoneApiRuntimeException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteCursor(String cursor) {
        try {
//            this.kintoneRecordManager.deleteCursor(cursor.getId()); // TODO: weida delete here
            this.recordClient.deleteCursor(cursor);
        }catch (KintoneApiRuntimeException e){
            this.logger.error(e.toString());
        }
    }
}
