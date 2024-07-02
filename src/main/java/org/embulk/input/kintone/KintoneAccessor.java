package org.embulk.input.kintone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.kintone.client.model.FileBody;
import com.kintone.client.model.Group;
import com.kintone.client.model.Organization;
import com.kintone.client.model.User;
import com.kintone.client.model.record.CalcFieldValue;
import com.kintone.client.model.record.CheckBoxFieldValue;
import com.kintone.client.model.record.DateFieldValue;
import com.kintone.client.model.record.DateTimeFieldValue;
import com.kintone.client.model.record.DropDownFieldValue;
import com.kintone.client.model.record.FieldType;
import com.kintone.client.model.record.FieldValue;
import com.kintone.client.model.record.FileFieldValue;
import com.kintone.client.model.record.GroupSelectFieldValue;
import com.kintone.client.model.record.LinkFieldValue;
import com.kintone.client.model.record.MultiLineTextFieldValue;
import com.kintone.client.model.record.MultiSelectFieldValue;
import com.kintone.client.model.record.NumberFieldValue;
import com.kintone.client.model.record.OrganizationSelectFieldValue;
import com.kintone.client.model.record.RadioButtonFieldValue;
import com.kintone.client.model.record.Record;
import com.kintone.client.model.record.RichTextFieldValue;
import com.kintone.client.model.record.SingleLineTextFieldValue;
import com.kintone.client.model.record.TableRow;
import com.kintone.client.model.record.TimeFieldValue;
import com.kintone.client.model.record.UserSelectFieldValue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public class KintoneAccessor
{
    private final Record record;
    private final String delimiter = "\n";
    private final Gson gson = createGson();

    public KintoneAccessor(final Record record)
    {
        this.record = record;
    }

    public String get(final String name)
    {
        return getAsString(name);
    }

    private String getAsString(final String fieldCode)
    {
        final FieldType fieldType = getFieldType(fieldCode);
        if (fieldType == null) {
            return null;
        }
        switch (fieldType) {
            case RECORD_NUMBER:
                return record.getRecordNumberFieldValue();
            case __ID__:
                return toString(record.getId(), Objects::toString);
            case __REVISION__:
                return toString(record.getRevision(), Objects::toString);
            case CREATOR:
                return toString(record.getCreatorFieldValue(), User::getCode);
            case CREATED_TIME:
                return toString(record.getCreatedTimeFieldValue(), (value) -> value.toInstant().toString());
            case MODIFIER:
                return toString(record.getModifierFieldValue(), User::getCode);
            case UPDATED_TIME:
                return toString(record.getUpdatedTimeFieldValue(), (value) -> value.toInstant().toString());
            case SINGLE_LINE_TEXT:
                return record.getSingleLineTextFieldValue(fieldCode);
            case MULTI_LINE_TEXT:
                return record.getMultiLineTextFieldValue(fieldCode);
            case RICH_TEXT:
                return record.getRichTextFieldValue(fieldCode);
            case NUMBER:
                return toString(record.getNumberFieldValue(fieldCode), BigDecimal::toString);
            case CALC:
                return record.getCalcFieldValue(fieldCode);
            case CHECK_BOX:
                return toString(record.getCheckBoxFieldValue(fieldCode));
            case RADIO_BUTTON:
                return record.getRadioButtonFieldValue(fieldCode);
            case MULTI_SELECT:
                return toString(record.getMultiSelectFieldValue(fieldCode));
            case DROP_DOWN:
                return record.getDropDownFieldValue(fieldCode);
            case USER_SELECT:
                return toString(record.getUserSelectFieldValue(fieldCode), User::getCode);
            case ORGANIZATION_SELECT:
                return toString(record.getOrganizationSelectFieldValue(fieldCode), Organization::getCode);
            case GROUP_SELECT:
                return toString(record.getGroupSelectFieldValue(fieldCode), Group::getCode);
            case DATE:
                return toString(record.getDateFieldValue(fieldCode), LocalDate::toString);
            case TIME:
                return toString(record.getTimeFieldValue(fieldCode), LocalTime::toString);
            case DATETIME:
                return toString(record.getDateTimeFieldValue(fieldCode), (value) -> value.toInstant().toString());
            case LINK:
                return record.getLinkFieldValue(fieldCode);
            case FILE:
                return toString(record.getFileFieldValue(fieldCode), FileBody::getFileKey);
            case SUBTABLE:
                return gson.toJson(record.getSubtableFieldValue(fieldCode));
            case CATEGORY:
                return toString(record.getCategoryFieldValue());
            case STATUS:
                return record.getStatusFieldValue();
            case STATUS_ASSIGNEE:
                return toString(record.getStatusAssigneeFieldValue(), User::getCode);
            // 以下は値を取得できないもの
            case REFERENCE_TABLE:
            case LABEL:
            case SPACER:
            case HR:
            case GROUP:
            default:
                return null;
        }
    }

    private FieldType getFieldType(final String fieldCode)
    {
        final FieldType fieldType = record.getFieldType(fieldCode);
        if (fieldType == null && "$id".equals(fieldCode)) {
            return FieldType.__ID__;
        }
        if (fieldType == null && "$revision".equals(fieldCode)) {
            return FieldType.__REVISION__;
        }
        return fieldType;
    }

    private <T> String toString(final T value, final Function<T, String> mapper)
    {
        return value == null ? null : mapper.apply(value);
    }

    private String toString(final List<String> list)
    {
        return list.stream()
                .filter(Objects::nonNull)
                .reduce((accum, value) -> accum + delimiter + value)
                .orElse("");
    }

    private <T> String toString(final List<T> list, final Function<T, String> mapper)
    {
        return list.stream()
                .filter(Objects::nonNull)
                .map(mapper)
                .filter(Objects::nonNull)
                .reduce((accum, value) -> accum + delimiter + value)
                .orElse("");
    }

    private Gson createGson()
    {
        final GsonBuilder builder = new GsonBuilder();
        registerTypeAdapter(builder, TableRow.class, this::serialize);
        registerTypeAdapter(builder, SingleLineTextFieldValue.class, this::serialize);
        registerTypeAdapter(builder, MultiLineTextFieldValue.class, this::serialize);
        registerTypeAdapter(builder, RichTextFieldValue.class, this::serialize);
        registerTypeAdapter(builder, NumberFieldValue.class, this::serialize);
        registerTypeAdapter(builder, CalcFieldValue.class, this::serialize);
        registerTypeAdapter(builder, CheckBoxFieldValue.class, this::serialize);
        registerTypeAdapter(builder, RadioButtonFieldValue.class, this::serialize);
        registerTypeAdapter(builder, MultiSelectFieldValue.class, this::serialize);
        registerTypeAdapter(builder, DropDownFieldValue.class, this::serialize);
        registerTypeAdapter(builder, UserSelectFieldValue.class, this::serialize);
        registerTypeAdapter(builder, OrganizationSelectFieldValue.class, this::serialize);
        registerTypeAdapter(builder, GroupSelectFieldValue.class, this::serialize);
        registerTypeAdapter(builder, DateFieldValue.class, this::serialize);
        registerTypeAdapter(builder, TimeFieldValue.class, this::serialize);
        registerTypeAdapter(builder, DateTimeFieldValue.class, this::serialize);
        registerTypeAdapter(builder, LinkFieldValue.class, this::serialize);
        registerTypeAdapter(builder, FileFieldValue.class, this::serialize);
        registerTypeAdapter(builder, FileBody.class, this::serialize);
        return builder.create();
    }

    private <T> void registerTypeAdapter(final GsonBuilder builder, final Class<T> type, final BiFunction<T, JsonSerializationContext, JsonElement> serialize)
    {
        builder.registerTypeAdapter(type, (JsonSerializer<T>) (src, typeOfSrc, context) -> serialize.apply(src, context));
    }

    private JsonElement serialize(final TableRow src, final JsonSerializationContext context)
    {
        final JsonObject object = new JsonObject();
        object.addProperty("id", src.getId());
        final JsonObject value = new JsonObject();
        src.getFieldCodes().forEach((code) -> value.add(code, context.serialize(src.getFieldValue(code))));
        object.add("value", value);
        return object;
    }

    private JsonElement serialize(final SingleLineTextFieldValue src, final JsonSerializationContext context)
    {
        return serialize(src, src.getValue());
    }

    private JsonElement serialize(final MultiLineTextFieldValue src, final JsonSerializationContext context)
    {
        return serialize(src, src.getValue());
    }

    private JsonElement serialize(final RichTextFieldValue src, final JsonSerializationContext context)
    {
        return serialize(src, src.getValue());
    }

    private JsonElement serialize(final NumberFieldValue src, final JsonSerializationContext context)
    {
        return serialize(src, src.getValue());
    }

    private JsonElement serialize(final CalcFieldValue src, final JsonSerializationContext context)
    {
        return serialize(src, src.getValue());
    }

    private JsonElement serialize(final CheckBoxFieldValue src, final JsonSerializationContext context)
    {
        return serialize(src, src.getValues(), context);
    }

    private JsonElement serialize(final RadioButtonFieldValue src, final JsonSerializationContext context)
    {
        return serialize(src, src.getValue());
    }

    private JsonElement serialize(final MultiSelectFieldValue src, final JsonSerializationContext context)
    {
        return serialize(src, src.getValues(), context);
    }

    private JsonElement serialize(final DropDownFieldValue src, final JsonSerializationContext context)
    {
        return serialize(src, src.getValue());
    }

    private JsonElement serialize(final UserSelectFieldValue src, final JsonSerializationContext context)
    {
        return serialize(src, src.getValues(), context);
    }

    private JsonElement serialize(final OrganizationSelectFieldValue src, final JsonSerializationContext context)
    {
        return serialize(src, src.getValues(), context);
    }

    private JsonElement serialize(final GroupSelectFieldValue src, final JsonSerializationContext context)
    {
        return serialize(src, src.getValues(), context);
    }

    private JsonElement serialize(final DateFieldValue src, final JsonSerializationContext context)
    {
        return serialize(src, src.getValue());
    }

    private JsonElement serialize(final TimeFieldValue src, final JsonSerializationContext context)
    {
        return serialize(src, src.getValue());
    }

    private JsonElement serialize(final DateTimeFieldValue src, final JsonSerializationContext context)
    {
        return serialize(src, toString(src.getValue(), (value) -> value.toInstant().toString()));
    }

    private JsonElement serialize(final LinkFieldValue src, final JsonSerializationContext context)
    {
        return serialize(src, src.getValue());
    }

    private JsonElement serialize(final FileFieldValue src, final JsonSerializationContext context)
    {
        return serialize(src, src.getValues(), context);
    }

    private JsonElement serialize(final FileBody src, final JsonSerializationContext context)
    {
        final JsonObject object = new JsonObject();
        object.addProperty("contentType", src.getContentType());
        object.addProperty("fileKey", src.getFileKey());
        object.addProperty("name", src.getName());
        object.addProperty("size", toString(src.getSize(), Objects::toString));
        return object;
    }

    private <T> JsonElement serialize(final FieldValue src, final T value)
    {
        final JsonObject object = new JsonObject();
        object.addProperty("type", src.getType().name());
        object.addProperty("value", toString(value, T::toString));
        return object;
    }

    private <T> JsonElement serialize(final FieldValue src, final List<T> values, final JsonSerializationContext context)
    {
        final JsonObject object = new JsonObject();
        object.addProperty("type", src.getType().name());
        final JsonArray array = new JsonArray();
        values.forEach((value) -> array.add(context.serialize(value)));
        object.add("value", array);
        return object;
    }
}
