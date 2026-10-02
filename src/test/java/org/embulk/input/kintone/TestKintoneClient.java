package org.embulk.input.kintone;

import com.kintone.client.AppClient;
import com.kintone.client.KintoneClientBuilder;
import com.kintone.client.RecordClient;
import com.kintone.client.exception.KintoneApiRuntimeException;
import com.kintone.client.exception.KintoneRuntimeException;
import com.kintone.client.model.app.field.FieldProperty;
import com.kintone.client.model.app.field.SingleLineTextFieldProperty;
import com.kintone.client.model.app.field.SubtableFieldProperty;
import com.kintone.client.model.record.FieldType;

import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.Schema;
import org.embulk.test.TestingEmbulk;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.net.ssl.SSLHandshakeException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestKintoneClient
{
    private ConfigSource config;
    private final KintoneClient client = new KintoneClient();
    private final AppClient appClient = mock(AppClient.class);
    private static final String BASIC_RESOURCE_PATH = "org/embulk/input/kintone/";
    private static final String SUCCESS_MSG = "Exception should be thrown by this";
    private final org.embulk.util.config.ConfigMapper configMapper = KintoneInputPlugin.CONFIG_MAPPER_FACTORY.createConfigMapper();

    private static final String CLIENT_CERTIFICATE_PASSWORD = "password";

    private KintoneClientBuilder builder;
    private RecordClient recordClient;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static ConfigSource loadYamlResource(TestingEmbulk embulk)
    {
        return embulk.loadYamlResource(BASIC_RESOURCE_PATH + "base.yml");
    }

    // Writes a PKCS#12 keystore protected by CLIENT_CERTIFICATE_PASSWORD into a temporary folder.
    // It is generated at test time so that no key material is committed to the repository.
    private String clientCertificatePath()
    {
        try {
            File file = new File(temporaryFolder.getRoot(), "client.pfx");
            if (!file.exists()) {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(null, null);
                try (OutputStream out = new FileOutputStream(file)) {
                    keyStore.store(out, CLIENT_CERTIFICATE_PASSWORD.toCharArray());
                }
            }
            return file.getAbsolutePath();
        }
        catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    // Returns a KintoneClient whose KintoneClientBuilder is a mock, so that connect() makes no network
    // access and the calls made on the builder can be verified. app() returns the shared appClient mock.
    private KintoneClient spyClient()
    {
        builder = mock(KintoneClientBuilder.class);
        com.kintone.client.KintoneClient kintoneClient = mock(com.kintone.client.KintoneClient.class);
        when(builder.build()).thenReturn(kintoneClient);
        when(kintoneClient.app()).thenReturn(appClient);
        recordClient = mock(RecordClient.class);
        when(kintoneClient.record()).thenReturn(recordClient);
        KintoneClient spy = spy(new KintoneClient());
        doReturn(builder).when(spy).newBuilder(anyString());
        return spy;
    }

    private static KintoneRuntimeException sslHandshakeFailure()
    {
        return new KintoneRuntimeException("Failed to request",
                new SSLHandshakeException("Received fatal alert: handshake_failure"));
    }

    // kintone Secure Access answers a request without a client certificate with this page (body abbreviated).
    private static final String NO_CERT_HTML = "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n<title>No Cert</title>\n"
            + "<style>body { background: url(data:image/png;base64,iVBORw0KGgo=); }</style></head><body></body></html>";

    private static KintoneApiRuntimeException htmlErrorResponse(int status, String html)
    {
        return new KintoneApiRuntimeException(status, Collections.emptyMap(), html);
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
    public void checkClientErrorLackingCertificatePassword()
    {
        config = loadYamlResource(embulk);
        config.set("client_certificate_path", clientCertificatePath());
        PluginTask task = configMapper.map(config, PluginTask.class);
        ConfigException e = assertThrows(ConfigException.class, () -> client.validateAuth(task));
        assertEquals("Client certificate and client certificate password must be provided together", e.getMessage());
    }

    @Test
    public void checkConnectErrorLackingCertificatePassword()
    {
        config = loadYamlResource(embulk);
        config.set("client_certificate_path", clientCertificatePath());
        PluginTask task = configMapper.map(config, PluginTask.class);
        KintoneClient client = new KintoneClient();
        ConfigException e = assertThrows(ConfigException.class, () -> client.connect(task));
        assertEquals("Client certificate and client certificate password must be provided together", e.getMessage());
    }

    @Test
    public void checkClientErrorInvalidCertificatePath()
    {
        config = loadYamlResource(embulk);
        config.set("client_certificate_path", "client\u0000.pfx");
        config.set("client_certificate_password", "password");
        PluginTask task = configMapper.map(config, PluginTask.class);
        ConfigException e = assertThrows(ConfigException.class, () -> client.validateAuth(task));
        assertEquals("Invalid client certificate path: client\u0000.pfx", e.getMessage());
    }

    @Test
    public void checkClientErrorLackingCertificate()
    {
        config = loadYamlResource(embulk);
        config.set("client_certificate_password", "password");
        PluginTask task = configMapper.map(config, PluginTask.class);
        ConfigException e = assertThrows(ConfigException.class, () -> client.validateAuth(task));
        assertEquals("Client certificate and client certificate password must be provided together", e.getMessage());
    }

    @Test
    public void checkClientErrorCertificateFileNotFound()
    {
        config = loadYamlResource(embulk);
        config.set("client_certificate_path", "/nonexistent/client.pfx");
        config.set("client_certificate_password", "password");
        PluginTask task = configMapper.map(config, PluginTask.class);
        ConfigException e = assertThrows(ConfigException.class, () -> client.validateAuth(task));
        assertEquals("Client certificate file not found or not readable: /nonexistent/client.pfx", e.getMessage());
    }

    @Test
    public void checkClientWithCertificate()
    {
        config = loadYamlResource(embulk);
        config.set("client_certificate_path", clientCertificatePath());
        config.set("client_certificate_password", CLIENT_CERTIFICATE_PASSWORD);
        PluginTask task = configMapper.map(config, PluginTask.class);
        Exception e = assertThrows(Exception.class, ()-> {
            client.validateAuth(task);
            throw new Exception(SUCCESS_MSG);
        });
        assertEquals(SUCCESS_MSG, e.getMessage());
    }

    @Test
    public void checkConnectWithCertificate()
    {
        config = loadYamlResource(embulk);
        config.set("client_certificate_path", clientCertificatePath());
        config.set("client_certificate_password", CLIENT_CERTIFICATE_PASSWORD);
        PluginTask task = configMapper.map(config, PluginTask.class);
        KintoneClient client = new KintoneClient();
        Exception e = assertThrows(Exception.class, ()-> {
            client.connect(task);
            throw new Exception(SUCCESS_MSG);
        });
        assertEquals(SUCCESS_MSG, e.getMessage());
    }

    @Test
    public void checkConnectErrorWrongCertificatePassword()
    {
        config = loadYamlResource(embulk);
        config.set("client_certificate_path", clientCertificatePath());
        config.set("client_certificate_password", "wrong-password");
        PluginTask task = configMapper.map(config, PluginTask.class);
        KintoneClient client = new KintoneClient();
        ConfigException e = assertThrows(ConfigException.class, () -> client.connect(task));
        assertTrue(e.getMessage().startsWith("Failed to load client certificate '" + clientCertificatePath() + "'."));
        assertFalse(e.getMessage().contains("wrong-password"));
    }

    @Test
    public void checkConnectErrorNotPkcs12() throws IOException
    {
        File notPkcs12 = temporaryFolder.newFile("not-a-certificate.pfx");
        Files.write(notPkcs12.toPath(), "not a PKCS#12 file".getBytes(StandardCharsets.UTF_8));
        config = loadYamlResource(embulk);
        config.set("client_certificate_path", notPkcs12.getAbsolutePath());
        config.set("client_certificate_password", CLIENT_CERTIFICATE_PASSWORD);
        PluginTask task = configMapper.map(config, PluginTask.class);
        KintoneClient client = new KintoneClient();
        ConfigException e = assertThrows(ConfigException.class, () -> client.connect(task));
        assertTrue(e.getMessage().startsWith("Failed to load client certificate '" + notPkcs12.getAbsolutePath() + "'."));
    }

    @Test
    public void checkConnectPassesCertificateToBuilder()
    {
        config = loadYamlResource(embulk);
        String path = clientCertificatePath();
        config.set("client_certificate_path", path);
        config.set("client_certificate_password", CLIENT_CERTIFICATE_PASSWORD);
        PluginTask task = configMapper.map(config, PluginTask.class);
        spyClient().connect(task);
        verify(builder).withClientCertificate(eq(Paths.get(path)), eq(CLIENT_CERTIFICATE_PASSWORD));
    }

    @Test
    public void checkConnectWithoutCertificateDoesNotUseCertificate()
    {
        config = loadYamlResource(embulk);
        PluginTask task = configMapper.map(config, PluginTask.class);
        spyClient().connect(task);
        verify(builder, never()).withClientCertificate(any(Path.class), anyString());
    }

    @Test
    public void checkSslHandshakeErrorWithCertificate()
    {
        config = loadYamlResource(embulk);
        String path = clientCertificatePath();
        config.set("client_certificate_path", path);
        config.set("client_certificate_password", CLIENT_CERTIFICATE_PASSWORD);
        PluginTask task = configMapper.map(config, PluginTask.class);
        KintoneClient client = spyClient();
        client.connect(task);
        doThrow(sslHandshakeFailure()).when(appClient).getFormFields(1);
        ConfigException e = assertThrows(ConfigException.class, () -> client.getFields(task));
        assertEquals("TLS handshake with https://dev.cybozu.com failed while using client certificate '" + path
                + "'. Check that the certificate was issued for this domain and is not expired or revoked.", e.getMessage());
        assertFalse(e.getMessage().contains(CLIENT_CERTIFICATE_PASSWORD));
    }

    @Test
    public void checkNoCertResponseWithoutCertificate()
    {
        config = loadYamlResource(embulk);
        config.set("domain", "example.s.cybozu.com");
        PluginTask task = configMapper.map(config, PluginTask.class);
        KintoneClient client = spyClient();
        client.connect(task);
        doThrow(htmlErrorResponse(400, NO_CERT_HTML)).when(appClient).getFormFields(1);
        ConfigException e = assertThrows(ConfigException.class, () -> client.getFields(task));
        assertEquals("kintone at https://example.s.cybozu.com rejected the request with HTTP 400 \"No Cert\"."
                + " This domain requires client_certificate_path and client_certificate_password.", e.getMessage());
        assertFalse(e.getCause().getMessage().contains("<html"));
    }

    @Test
    public void checkNoCertResponseWithCertificate()
    {
        config = loadYamlResource(embulk);
        String path = clientCertificatePath();
        config.set("client_certificate_path", path);
        config.set("client_certificate_password", CLIENT_CERTIFICATE_PASSWORD);
        PluginTask task = configMapper.map(config, PluginTask.class);
        KintoneClient client = spyClient();
        client.connect(task);
        doThrow(htmlErrorResponse(400, NO_CERT_HTML)).when(appClient).getFormFields(1);
        ConfigException e = assertThrows(ConfigException.class, () -> client.getFields(task));
        assertEquals("kintone at https://dev.cybozu.com rejected the request with HTTP 400 \"No Cert\" even though client_certificate_path '"
                + path + "' is set. Check that the certificate was issued for this domain.", e.getMessage());
    }

    @Test
    public void checkNoCertResponseOnCreateCursor()
    {
        config = loadYamlResource(embulk);
        PluginTask task = configMapper.map(config, PluginTask.class);
        KintoneClient client = spyClient();
        client.connect(task);
        doThrow(htmlErrorResponse(400, NO_CERT_HTML)).when(recordClient).createCursor(any());
        ConfigException e = assertThrows(ConfigException.class, () -> client.createCursor(task, task.getFields().toSchema()));
        assertTrue(e.getMessage().startsWith("kintone at https://dev.cybozu.com rejected the request with HTTP 400 \"No Cert\"."));
    }

    @Test
    public void checkHtmlErrorResponseIsSummarized()
    {
        config = loadYamlResource(embulk);
        PluginTask task = configMapper.map(config, PluginTask.class);
        KintoneClient client = spyClient();
        client.connect(task);
        doThrow(htmlErrorResponse(503, "<html><head><title>Service Unavailable</title></head><body>huge</body></html>"))
                .when(appClient).getFormFields(1);
        RuntimeException e = assertThrows(RuntimeException.class, () -> client.getFields(task));
        assertEquals("HTTP error status 503 from https://dev.cybozu.com: Service Unavailable", e.getMessage());
        assertEquals("HTTP error status 503, HTML page \"Service Unavailable\"", e.getCause().getMessage());
    }

    @Test
    public void checkJsonApiErrorIsUnchanged()
    {
        config = loadYamlResource(embulk);
        PluginTask task = configMapper.map(config, PluginTask.class);
        KintoneClient client = spyClient();
        client.connect(task);
        KintoneApiRuntimeException failure = htmlErrorResponse(404, "{\"code\":\"GAIA_CN01\",\"message\":\"cursor\"}");
        doThrow(failure).when(appClient).getFormFields(1);
        KintoneApiRuntimeException e = assertThrows(KintoneApiRuntimeException.class, () -> client.getFields(task));
        assertSame(failure, e);
    }

    @Test
    public void checkSslHandshakeErrorWithoutCertificateIsNotRewritten()
    {
        config = loadYamlResource(embulk);
        PluginTask task = configMapper.map(config, PluginTask.class);
        KintoneClient client = spyClient();
        client.connect(task);
        KintoneRuntimeException failure = sslHandshakeFailure();
        doThrow(failure).when(appClient).getFormFields(1);
        KintoneRuntimeException e = assertThrows(KintoneRuntimeException.class, () -> client.getFields(task));
        assertSame(failure, e);
    }

    @Test
    public void checkNonSslErrorIsNotRewritten()
    {
        config = loadYamlResource(embulk);
        config.set("client_certificate_path", clientCertificatePath());
        config.set("client_certificate_password", CLIENT_CERTIFICATE_PASSWORD);
        PluginTask task = configMapper.map(config, PluginTask.class);
        KintoneClient client = spyClient();
        client.connect(task);
        KintoneRuntimeException failure = new KintoneRuntimeException("Failed to request", new IOException("Connection reset"));
        doThrow(failure).when(appClient).getFormFields(1);
        KintoneRuntimeException e = assertThrows(KintoneRuntimeException.class, () -> client.getFields(task));
        assertSame(failure, e);
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

    @Test
    public void testCreateCursorThrowsExceptionWhenCursorAlreadyExists()
    {
        config = loadYamlResource(embulk);
        PluginTask task = configMapper.map(config, PluginTask.class);
        Schema schema = mock(Schema.class);

        KintoneClient client = new KintoneClient(appClient);

        try {
            Field cursorIdField = KintoneClient.class.getDeclaredField("cursorId");
            cursorIdField.setAccessible(true);
            cursorIdField.set(client, "existingCursorId");

            Field cursorAlreadyExistsErrorField = KintoneClient.class.getDeclaredField("CURSOR_ALREADY_EXISTS_ERROR");
            cursorAlreadyExistsErrorField.setAccessible(true);
            String cursorAlreadyExistsErrorMessage = (String) cursorAlreadyExistsErrorField.get(null);

            Method createCursorMethod = KintoneClient.class.getDeclaredMethod("createCursor", PluginTask.class, Schema.class);
            createCursorMethod.setAccessible(true);

            Exception exception = assertThrows(RuntimeException.class, () -> {
                try {
                    createCursorMethod.invoke(client, task, schema);
                }
                catch (InvocationTargetException ex) {
                    throw ex.getTargetException();
                }
            });

            assertEquals(cursorAlreadyExistsErrorMessage, exception.getMessage());
        }
        catch (Exception e) {
            throw new RuntimeException("Reflection error", e);
        }
    }
}
