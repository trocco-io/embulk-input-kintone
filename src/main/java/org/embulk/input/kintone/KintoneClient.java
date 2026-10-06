package org.embulk.input.kintone;

import com.google.common.annotations.VisibleForTesting;
import com.kintone.client.AppClient;
import com.kintone.client.KintoneClientBuilder;
import com.kintone.client.RecordClient;
import com.kintone.client.api.record.CreateCursorRequest;
import com.kintone.client.api.record.CreateCursorResponseBody;
import com.kintone.client.api.record.GetRecordsByCursorResponseBody;
import com.kintone.client.exception.KintoneApiRuntimeException;
import com.kintone.client.exception.KintoneRuntimeException;
import com.kintone.client.model.app.field.FieldProperty;
import com.kintone.client.model.app.field.SubtableFieldProperty;
import com.kintone.client.model.record.FieldType;
import org.embulk.config.ConfigException;
import org.embulk.spi.Column;
import org.embulk.spi.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KintoneClient implements AutoCloseable
{
    private final Logger logger = LoggerFactory.getLogger(KintoneClient.class);
    private static final int FETCH_SIZE = 500;
    private static final String CURSOR_ALREADY_EXISTS_ERROR = "Cursor already exists: KintoneClient can only generate one cursor per instance.";
    private static final String CLIENT_CERTIFICATE_PASSWORD_WITHOUT_PATH_MESSAGE =
            "client_certificate_password requires client_certificate_path";
    private static final Pattern HTML_TITLE = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final int HTML_TITLE_MAX_LENGTH = 200;
    private RecordClient recordClient;
    private AppClient appClient;
    private String cursorId;

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
        if (task.getClientCertificatePassword().isPresent() && !task.getClientCertificatePath().isPresent()) {
            throw new ConfigException(CLIENT_CERTIFICATE_PASSWORD_WITHOUT_PATH_MESSAGE);
        }
        if (task.getClientCertificatePath().isPresent()) {
            validateClientCertificatePath(task.getClientCertificatePath().get());
        }
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

    private static Path clientCertificatePath(final String path) throws ConfigException
    {
        try {
            return Paths.get(path);
        }
        catch (InvalidPathException e) {
            throw new ConfigException(String.format("Invalid client certificate path: %s", path), e);
        }
    }

    private static void validateClientCertificatePath(final String path) throws ConfigException
    {
        final Path certificate = clientCertificatePath(path);
        if (!Files.isRegularFile(certificate) || !Files.isReadable(certificate)) {
            throw new ConfigException(String.format("Client certificate file not found or not readable: %s", path));
        }
    }

    public void connect(final PluginTask task)
    {
        KintoneClientBuilder builder = newBuilder(String.format("https://%s", task.getDomain()));
        if (task.getUsername().isPresent() && task.getPassword().isPresent()) {
            builder.authByPassword(task.getUsername().get(), task.getPassword().get());
        }
        else if (task.getToken().isPresent()) {
            builder.authByApiToken(task.getToken().get());
        }

        if (task.getBasicAuthUsername().isPresent() && task.getBasicAuthPassword().isPresent()) {
            builder.withBasicAuth(task.getBasicAuthUsername().get(), task.getBasicAuthPassword().get());
        }

        if (task.getClientCertificatePath().isPresent()) {
            final String path = task.getClientCertificatePath().get();
            // A certificate without a password is configured by omitting client_certificate_password or by setting
            // it to ""; both are treated the same.
            final String password = task.getClientCertificatePassword().orElse("");
            final Path certificate = clientCertificatePath(path);
            try {
                builder.withClientCertificate(certificate, password);
            }
            catch (KintoneRuntimeException e) {
                throw new ConfigException(String.format(
                        "Failed to load client certificate '%s'. Make sure the file is a valid PKCS#12 (.pfx) and the password is correct.",
                        path), e);
            }
        }

        if (task.getGuestSpaceId().isPresent()) {
            builder.setGuestSpaceId(task.getGuestSpaceId().orElse(-1));
        }

        com.kintone.client.KintoneClient client = builder.build();
        this.recordClient = client.record();
        this.appClient = client.app();
    }

    @VisibleForTesting
    protected KintoneClientBuilder newBuilder(final String baseUrl)
    {
        return KintoneClientBuilder.create(baseUrl);
    }

    // Explains failures that involve the client certificate as configuration problems; anything else is
    // returned as is.
    // - kintone Secure Access answers a request without a client certificate with HTTP 400 and an HTML page
    //   titled "No Cert" (the TLS handshake itself succeeds).
    // - kintone-java-client wraps I/O failures (including SSLHandshakeException) as
    //   KintoneRuntimeException("Failed to request", cause). Only a failed handshake is treated as a certificate
    //   problem; other TLS errors (for example a connection reset after the handshake) can be transient and are
    //   returned as is.
    // HTML error pages are also summarized to their <title> so that the page body is kept out of the log.
    private static RuntimeException withClientCertificateHint(final KintoneRuntimeException e, final PluginTask task)
    {
        if (e instanceof KintoneApiRuntimeException) {
            return describeHtmlErrorResponse((KintoneApiRuntimeException) e, task);
        }
        if (hasSslHandshakeCause(e) && task.getClientCertificatePath().isPresent()) {
            return new ConfigException(String.format(
                    "TLS handshake with https://%s failed while using client certificate '%s'. Check that the certificate was issued for this domain and is not expired or revoked, or whether another TLS problem (for example a proxy or trust store) is the cause.",
                    task.getDomain(), task.getClientCertificatePath().get()), e);
        }
        return e;
    }

    private static RuntimeException describeHtmlErrorResponse(final KintoneApiRuntimeException e, final PluginTask task)
    {
        final String title = htmlTitle(e.getContent());
        if (title == null) {
            return e;
        }
        final KintoneApiRuntimeException summary = summarizeHtmlErrorResponse(e);
        final String domain = task.getDomain();
        if (e.getStatusCode() == 400 && "No Cert".equals(title)) {
            if (task.getClientCertificatePath().isPresent()) {
                return new ConfigException(String.format(
                        "kintone at https://%s rejected the request with HTTP 400 \"No Cert\" even though client_certificate_path '%s' is set. Check that the certificate was issued for this domain.",
                        domain, task.getClientCertificatePath().get()), summary);
            }
            return new ConfigException(String.format(
                    "kintone at https://%s rejected the request with HTTP 400 \"No Cert\". This domain requires client_certificate_path and client_certificate_password.",
                    domain), summary);
        }
        return new RuntimeException(String.format("HTTP error status %d from https://%s: %s", e.getStatusCode(), domain, title), summary);
    }

    // Returns a copy of e whose message carries only the HTTP status and the HTML <title>, so that the page body
    // (which can embed images) is kept out of the message, the stack trace and the log. Returns e itself when
    // the response is not HTML.
    @VisibleForTesting
    static KintoneApiRuntimeException summarizeHtmlErrorResponse(final KintoneApiRuntimeException e)
    {
        final String title = htmlTitle(e.getContent());
        if (title == null) {
            return e;
        }
        return new KintoneApiRuntimeException(e.getStatusCode(), e.getHeaders(), String.format("HTML page \"%s\"", title));
    }

    private static String htmlTitle(final String content)
    {
        if (content == null || !content.trim().startsWith("<")) {
            return null;
        }
        final Matcher matcher = HTML_TITLE.matcher(content);
        if (!matcher.find()) {
            return "";
        }
        // The title can span lines and has no length limit; keep the message and the log to one short line.
        final String title = matcher.group(1).replaceAll("\\s+", " ").trim();
        return title.length() <= HTML_TITLE_MAX_LENGTH ? title : title.substring(0, HTML_TITLE_MAX_LENGTH) + "...";
    }

    // Routes a kintone API error: HTML error pages are explained or summarized without logging the page body,
    // JSON API errors are logged and wrapped as before.
    private RuntimeException apiError(final KintoneApiRuntimeException e, final PluginTask task)
    {
        final RuntimeException hinted = withClientCertificateHint(e, task);
        if (hinted != e) {
            return hinted;
        }
        this.logger.error(e.toString());
        return new RuntimeException(e);
    }

    private static boolean hasSslHandshakeCause(final Throwable e)
    {
        for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof SSLHandshakeException) {
                return true;
            }
        }
        return false;
    }

    public GetRecordsByCursorResponseBody getResponse(final PluginTask task, final Schema schema)
    {
        this.createCursor(task, schema);
        try {
            return this.recordClient.getRecordsByCursor(this.cursorId);
        }
        catch (KintoneApiRuntimeException e) {
            throw apiError(e, task);
        }
        catch (KintoneRuntimeException e) {
            throw withClientCertificateHint(e, task);
        }
    }

    public GetRecordsByCursorResponseBody getRecordsByCursor(final PluginTask task)
    {
        try {
            return this.recordClient.getRecordsByCursor(this.cursorId);
        }
        catch (KintoneApiRuntimeException e) {
            throw apiError(e, task);
        }
        catch (KintoneRuntimeException e) {
            throw withClientCertificateHint(e, task);
        }
    }

    public void createCursor(final PluginTask task, final Schema schema)
    {
        if (this.cursorId != null) {
            throw new RuntimeException(CURSOR_ALREADY_EXISTS_ERROR);
        }
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
            CreateCursorResponseBody cursorResponse = recordClient.createCursor(request);
            this.cursorId = cursorResponse.getId();
        }
        catch (KintoneApiRuntimeException e) {
            throw apiError(e, task);
        }
        catch (KintoneRuntimeException e) {
            throw withClientCertificateHint(e, task);
        }
    }

    private void deleteCursor()
    {
        if (this.cursorId != null) {
            try {
                this.recordClient.deleteCursor(this.cursorId);
                this.cursorId = null;
            }
            catch (KintoneApiRuntimeException e) {
                this.logger.error(summarizeHtmlErrorResponse(e).toString());
            }
        }
    }

    public Map<String, FieldProperty> getFields(final PluginTask task)
    {
        Map<String, FieldProperty> fields = getFormFields(task);
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
        Map<String, FieldProperty> fields = getFormFields(task);
        for (Map.Entry<String, FieldProperty> entry : fields.entrySet()) {
            if (entry.getValue().getType() == fieldType) {
                fieldCodes.add(entry.getKey());
            }
        }
        return fieldCodes;
    }

    private Map<String, FieldProperty> getFormFields(final PluginTask task)
    {
        try {
            return this.appClient.getFormFields(task.getAppId());
        }
        catch (KintoneRuntimeException e) {
            throw withClientCertificateHint(e, task);
        }
    }

    @Override
    public void close()
    {
        this.deleteCursor();
    }
}
