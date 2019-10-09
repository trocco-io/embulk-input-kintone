package org.embulk.input.kintone;

import com.cybozu.kintone.client.authentication.Auth;
import com.cybozu.kintone.client.connection.Connection;
import com.cybozu.kintone.client.model.record.GetRecordsResponse;
import com.cybozu.kintone.client.module.record.Record;
import org.embulk.config.ConfigException;
import org.embulk.spi.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class KintoneClient {
    private final Logger logger = LoggerFactory.getLogger(KintoneClient.class);
    private final PluginTask pluginTask;
    private ArrayList<String> fields;
    private Auth kintoneAuth;
    private Record kintoneRecordManager;
    private Connection con;

    public KintoneClient(final PluginTask pluginTask) {
        this.pluginTask = pluginTask;
        this.kintoneAuth = new Auth();

        this.fields = new ArrayList<String>();
        for (ColumnConfig c : pluginTask.getFields().getColumns()
        ) {
            fields.add(c.getName());
        }
        this.setAuth();
        if (pluginTask.getGuestSpaceId().isPresent()) {
            this.con = new Connection(pluginTask.getDomain(), this.kintoneAuth, pluginTask.getGuestSpaceId().or(-1));
        } else {
            this.con = new Connection(pluginTask.getDomain(), this.kintoneAuth);
        }
        this.kintoneRecordManager = new Record(con);
    }

    private void setAuth() {
        if (pluginTask.getUsername().isPresent() && pluginTask.getPassword().isPresent()) {
            this.kintoneAuth.setPasswordAuth(pluginTask.getUsername().get(), pluginTask.getPassword().get());
        } else if (pluginTask.getToken().isPresent()) {
            this.kintoneAuth.setApiToken(pluginTask.getToken().get());
        } else {
            throw new ConfigException("Username and password or token must be provided");
        }

        if (pluginTask.getBasicAuthUsername().isPresent() && pluginTask.getBasicAuthPassword().isPresent()) {
            this.kintoneAuth.setBasicAuth(pluginTask.getBasicAuthUsername().get(),
                    pluginTask.getBasicAuthPassword().get());
        }
    }

    public GetRecordsResponse getResponse() {
        try {
            return kintoneRecordManager.getAllRecordsByQuery(
                    this.pluginTask.getAppId(), this.pluginTask.getQuery().or(""), this.fields);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
