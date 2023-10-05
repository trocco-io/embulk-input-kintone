package org.embulk.input.kintone;

import org.embulk.util.config.Config;
import org.embulk.util.config.ConfigDefault;
import org.embulk.util.config.Task;
import org.embulk.util.config.units.SchemaConfig;

import java.util.Optional;

public interface PluginTask
        extends Task
{
    @Config("domain")
    String getDomain();

    @Config("app_id")
    int getAppId();

    @Config("guest_space_id")
    @ConfigDefault("null")
    Optional<Integer> getGuestSpaceId();

    @Config("token")
    @ConfigDefault("null")
    Optional<String> getToken();

    @Config("username")
    @ConfigDefault("null")
    Optional<String> getUsername();

    @Config("password")
    @ConfigDefault("null")
    Optional<String> getPassword();

    @Config("basic_auth_username")
    @ConfigDefault("null")
    Optional<String> getBasicAuthUsername();

    @Config("basic_auth_password")
    @ConfigDefault("null")
    Optional<String> getBasicAuthPassword();

    @Config("query")
    @ConfigDefault("null")
    Optional<String> getQuery();

    @Config("expand_subtable")
    @ConfigDefault("false")
    boolean getExpandSubtable();

    @Config("fields")
    SchemaConfig getFields();
}
