package org.embulk.input.kintone;

import com.cybozu.kintone.client.model.app.form.FieldType;
import com.cybozu.kintone.client.model.record.field.FieldValue;

import java.util.HashMap;

public class TestHelper {
    public static HashMap<String, FieldValue> addField(HashMap<String, FieldValue> record, String code, FieldType type,
                                                 Object value) {
        FieldValue newField = new FieldValue();
        newField.setType(type);
        newField.setValue(value);
        record.put(code, newField);
        return record;
    }
}
