package org.embulk.input.kintone;

import com.cybozu.kintone.client.authentication.Auth;
import com.cybozu.kintone.client.connection.Connection;
import com.cybozu.kintone.client.exception.KintoneAPIException;
import com.cybozu.kintone.client.model.cursor.CreateRecordCursorResponse;
import com.cybozu.kintone.client.model.record.GetRecordsResponse;
import com.cybozu.kintone.client.module.recordCursor.RecordCursor;
import org.embulk.config.ConfigException;
import org.embulk.spi.ColumnConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class KintoneClient {
    private final Logger logger = LoggerFactory.getLogger(KintoneClient.class);
<<<<<<< HEAD
=======
    private static final int FETCH_SIZE = 500;
    private final PluginTask task;
    private ArrayList<String> fields;
>>>>>>> use cursor to fetch records
    private Auth kintoneAuth;
    private RecordCursor kintoneRecordManager;
    private Connection con;
    private CreateRecordCursorResponse cursor;

    public KintoneClient(){
        this.kintoneAuth = new Auth();
    }

    public void validateAuth(final PluginTask task) throws ConfigException{
        if (task.getUsername().isPresent() && task.getPassword().isPresent()) {
            return;
        } else if (task.getToken().isPresent()) {
            return;
        } else {
            throw new ConfigException("Username and password or token must be provided");
        }
        this.kintoneRecordManager = new RecordCursor(con);
    }

    public void connect(final PluginTask task) {
        if (task.getUsername().isPresent() && task.getPassword().isPresent()) {
            this.kintoneAuth.setPasswordAuth(task.getUsername().get(), task.getPassword().get());
        } else if (task.getToken().isPresent()) {
            this.kintoneAuth.setApiToken(task.getToken().get());
        }

        if (task.getBasicAuthUsername().isPresent() && task.getBasicAuthPassword().isPresent()) {
            this.kintoneAuth.setBasicAuth(task.getBasicAuthUsername().get(),
                    task.getBasicAuthPassword().get());
        }

        if (task.getGuestSpaceId().isPresent()) {
            this.con = new Connection(task.getDomain(), this.kintoneAuth, task.getGuestSpaceId().or(-1));
        } else {
            this.con = new Connection(task.getDomain(), this.kintoneAuth);
        }
        this.kintoneRecordManager = new Record(con);
    }


    public GetRecordsResponse getResponse(final PluginTask task) {
        ArrayList<String> fields = new ArrayList<>();
        for (ColumnConfig c : task.getFields().getColumns()
        ) {
            fields.add(c.getName());
        }
        try {
            this.cursor = this.kintoneRecordManager.createCursor(this.task.getAppId(),
                    this.fields, this.task.getQuery().or(""), FETCH_SIZE);
            return this.kintoneRecordManager.getAllRecords(cursor.getId());
        }catch (KintoneAPIException e){
            this.logger.error(e.toString());
            throw new RuntimeException(e);
        }
    }

    public void deleteCursor() {
        try {
            this.kintoneRecordManager.deleteCursor(this.cursor.getId());
        }catch (KintoneAPIException e){
            this.logger.error(e.toString());
            throw new RuntimeException(e);
        }
    }
}
