package org.embulk.input.kintone;

import com.kintone.client.AppClient;
import com.kintone.client.model.app.field.FieldProperty;
import com.kintone.client.model.app.field.SingleLineTextFieldProperty;
import com.kintone.client.model.app.field.SubtableFieldProperty;
import com.kintone.client.model.record.FieldType;

import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.spi.InputPlugin;
import org.embulk.test.TestingEmbulk;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class TestKintoneClient
{
    private ConfigSource config;
    private final KintoneClient client = new KintoneClient();
    private final AppClient appClient = mock(AppClient.class);
    private static final String BASIC_RESOURCE_PATH = "org/embulk/input/kintone/";
    private static final String SUCCESS_MSG = "Exception should be thrown by this";
    private final org.embulk.util.config.ConfigMapper configMapper = KintoneInputPlugin.CONFIG_MAPPER_FACTORY.createConfigMapper();

    private static ConfigSource loadYamlResource(TestingEmbulk embulk)
    {
        return embulk.loadYamlResource(BASIC_RESOURCE_PATH + "base.yml");
    }

    @Rule
    public TestingEmbulk embulk = TestingEmbulk.builder()
            .registerPlugin(InputPlugin.class, "kintone", KintoneInputPlugin.class)
            .build();

    @Test
    public void checkClientWithUsernameAndPassword()
    {
        config = loadYamlResource(embulk);
        PluginTask task = configMapper.map(config, PluginTask.class);
        Exception e = assertThrows(Exception.class, ()-> {
            client.validateAuth(task);
            throw new Exception(SUCCESS_MSG);
        });
        assertEquals(SUCCESS_MSG, e.getMessage());
    }

    @Test
    public void checkThrowErrorWithoutAuthInfo()
    {
        config = loadYamlResource(embulk);
        config.remove("username")
                .remove("password");
        PluginTask task = configMapper.map(config, PluginTask.class);
        ConfigException e = assertThrows(ConfigException.class, () -> client.validateAuth(task));
        assertEquals("Username and password or token must be provided", e.getMessage());
    }

    @Test
    public void checkClientErrorLackingPassword()
    {
        config = loadYamlResource(embulk);
        config.remove("password");
        PluginTask task = configMapper.map(config, PluginTask.class);
        ConfigException e = assertThrows(ConfigException.class, () -> client.validateAuth(task));
        assertEquals("Username and password or token must be provided", e.getMessage());
    }

    @Test
    public void checkClientErrorLackingUsername()
    {
        config = loadYamlResource(embulk);
        config.remove("username");
        PluginTask task = configMapper.map(config, PluginTask.class);
        ConfigException e = assertThrows(ConfigException.class, () -> client.validateAuth(task));
        assertEquals("Username and password or token must be provided", e.getMessage());
    }

    @Test
    public void checkClientWithToken()
    {
        config = loadYamlResource(embulk);
        config.remove("username")
                .remove("password")
                .set("token", "token");
        PluginTask task = configMapper.map(config, PluginTask.class);
        Exception e = assertThrows(Exception.class, ()-> {
            client.validateAuth(task);
            throw new Exception(SUCCESS_MSG);
        });
        assertEquals(SUCCESS_MSG, e.getMessage());
    }

    @Test
    public void checkGetFieldsWithoutExpand()
    {
        config = loadYamlResource(embulk);
        PluginTask task = configMapper.map(config, PluginTask.class);

        KintoneClient client = new KintoneClient(appClient);

        Map<String, FieldProperty> fields = new HashMap<>();
        fields.put("single line1", new SingleLineTextFieldProperty());

        SubtableFieldProperty subtableFieldProperty = new SubtableFieldProperty();
        Map<String, FieldProperty> subTableFields = new HashMap<>();
        subTableFields.put("single line2-1", new SingleLineTextFieldProperty());
        subTableFields.put("single line2-2", new SingleLineTextFieldProperty());
        subtableFieldProperty.setFields(subTableFields);
        fields.put("subtable2", subtableFieldProperty);
        doReturn(fields).when(appClient).getFormFields(1);

        assertEquals(2, client.getFields(task).size());
        assertTrue(client.getFields(task).keySet().contains("single line1"));
        assertTrue(client.getFields(task).keySet().contains("subtable2"));
    }

    @Test
    public void checkGetFieldsWithExpand()
    {
        config = loadYamlResource(embulk);
        config.set("expand_subtable", true);
        PluginTask task = configMapper.map(config, PluginTask.class);

        KintoneClient client = new KintoneClient(appClient);

        Map<String, FieldProperty> fields = new HashMap<>();
        fields.put("single line1", new SingleLineTextFieldProperty());

        SubtableFieldProperty subtableFieldProperty = new SubtableFieldProperty();
        Map<String, FieldProperty> subTableFields = new HashMap<>();
        subTableFields.put("single line2-1", new SingleLineTextFieldProperty());
        subTableFields.put("single line2-2", new SingleLineTextFieldProperty());
        subtableFieldProperty.setFields(subTableFields);
        fields.put("subtable2", subtableFieldProperty);
        doReturn(fields).when(appClient).getFormFields(1);

        assertEquals(3, client.getFields(task).size());
        assertTrue(client.getFields(task).keySet().contains("single line1"));
        assertTrue(client.getFields(task).keySet().contains("single line2-1"));
        assertTrue(client.getFields(task).keySet().contains("single line2-2"));
    }

    @Test
    public void checkGetFieldCodes()
    {
        config = loadYamlResource(embulk);
        config.set("expand_subtable", true);
        PluginTask task = configMapper.map(config, PluginTask.class);

        KintoneClient client = new KintoneClient(appClient);

        Map<String, FieldProperty> fields = new HashMap<>();
        fields.put("single line1", new SingleLineTextFieldProperty());
        fields.put("subtable2",  new SubtableFieldProperty());
        doReturn(fields).when(appClient).getFormFields(1);

        assertEquals(1, client.getFieldCodes(task, FieldType.SUBTABLE).size());
        assertTrue(client.getFieldCodes(task, FieldType.SUBTABLE).contains("subtable2"));
    }
}
