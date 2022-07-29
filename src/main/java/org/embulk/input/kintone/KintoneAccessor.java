package org.embulk.input.kintone;

import com.google.gson.Gson;
import com.kintone.client.model.FileBody;
import com.kintone.client.model.Group;
import com.kintone.client.model.Organization;
import com.kintone.client.model.User;
import com.kintone.client.model.record.Record;

import java.util.List;
import java.util.function.Function;

public class KintoneAccessor
{
    private final Gson gson = new Gson();

    private final Record record;
    private final String delimiter = "\n";

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
        switch (record.getFieldType(fieldCode)) {
            case USER_SELECT:
                return toString(record.getUserSelectFieldValue(fieldCode), User::getCode);
            case ORGANIZATION_SELECT:
                return toString(record.getOrganizationSelectFieldValue(fieldCode), Organization::getCode);
            case GROUP_SELECT:
                return toString(record.getGroupSelectFieldValue(fieldCode), Group::getCode);
            case STATUS_ASSIGNEE:
                return toString(record.getStatusAssigneeFieldValue(), User::getCode);
            case SUBTABLE:
                return gson.toJson(record.getSubtableFieldValue(fieldCode));
            case CREATOR:
                return record.getCreatorFieldValue().getCode();
            case MODIFIER:
                return record.getModifierFieldValue().getCode();
            case CHECK_BOX:
                return toString(record.getCheckBoxFieldValue(fieldCode));
            case MULTI_SELECT:
                return toString(record.getMultiSelectFieldValue(fieldCode));
            case CATEGORY:
                return toString(record.getCategoryFieldValue());
            case FILE:
                return toString(record.getFileFieldValue(fieldCode), FileBody::getFileKey);
            case NUMBER:
                return String.valueOf(record.getNumberFieldValue(fieldCode));
            // 以上は既存で明示的に処理していたもの
            case CALC:
                return String.valueOf(record.getCalcFieldValue(fieldCode));
            case DROP_DOWN:
                return record.getDropDownFieldValue(fieldCode);
            case LINK:
                return record.getLinkFieldValue(fieldCode);
            case MULTI_LINE_TEXT:
                return record.getMultiLineTextFieldValue(fieldCode);
            case RADIO_BUTTON:
                return record.getRadioButtonFieldValue(fieldCode);
            case RECORD_NUMBER:
                return record.getRecordNumberFieldValue();
            case RICH_TEXT:
                return record.getRichTextFieldValue(fieldCode);
            case SINGLE_LINE_TEXT:
                return record.getSingleLineTextFieldValue(fieldCode);
            case STATUS:
                return record.getStatusFieldValue();
            case DATE:
                return String.valueOf(record.getDateFieldValue(fieldCode));
            case TIME:
                return String.valueOf(record.getTimeFieldValue(fieldCode));
            case DATETIME:
                return String.valueOf(record.getDateTimeFieldValue(fieldCode));
            case CREATED_TIME:
                return String.valueOf(record.getCreatedTimeFieldValue());
            case UPDATED_TIME:
                return String.valueOf(record.getUpdatedTimeFieldValue());
            case __ID__:
                return String.valueOf(record.getId());
            case __REVISION__:
                return String.valueOf(record.getRevision());
            // 以下は値を取得できないもの
            case GROUP:
            case HR:
            case LABEL:
            case REFERENCE_TABLE:
            case SPACER:
            default:
                return "";
        }
    }

    private String toString(List<String> list)
    {
        return list.stream()
                .reduce((accum, value) -> accum + delimiter + value)
                .orElse("");
    }

    private <T> String toString(List<T> list, Function<T, String> mapper)
    {
        return list.stream().map(mapper)
                .reduce((accum, value) -> accum + delimiter + value)
                .orElse("");
    }
}
