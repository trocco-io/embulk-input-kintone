package org.embulk.input.kintone;

import com.cybozu.kintone.client.model.file.FileModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.cybozu.kintone.client.model.record.field.FieldValue;
import com.cybozu.kintone.client.model.member.Member;

import java.util.ArrayList;
import java.util.HashMap;

public class KintoneAccessor {
    private final Logger logger = LoggerFactory.getLogger(KintoneAccessor.class);

    private final HashMap<String, FieldValue> record;
    private final String delimiter = "\n";

    public KintoneAccessor(final HashMap<String, FieldValue> record) {
        this.record = record;
    }

    public String get(String name) {
        switch (this.record.get(name).getType()) {
            case USER_SELECT:
            case ORGANIZATION_SELECT:
            case GROUP_SELECT:
            case STATUS_ASSIGNEE:
                ArrayList<Member> members = (ArrayList<Member>) this.record.get(name).getValue();
                return members.stream().map(Member::getCode)
                        .reduce((accum, value) -> accum + this.delimiter + value)
                        .orElse("");
            case SUBTABLE:
                // TODO: support sub table
                return "";
            case CREATOR:
            case MODIFIER:
                Member m = (Member) this.record.get(name).getValue();
                return m.getCode();
            case CHECK_BOX:
            case MULTI_SELECT:
            case CATEGORY:
                ArrayList<String> selectedItemList = (ArrayList<String>) this.record.get(name).getValue();
                return selectedItemList.stream()
                        .reduce((accum, value) -> accum + this.delimiter + value)
                        .orElse("");
            case FILE:
                ArrayList<FileModel> cbFileList = (ArrayList<FileModel>) this.record.get(name).getValue();
                return cbFileList.stream().map(FileModel::getFileKey)
                        .reduce((accum, value) -> accum + this.delimiter + value)
                        .orElse("");
            case NUMBER:
                return  String.valueOf(this.record.get(name).getValue());
            default:
                return (String) this.record.get(name).getValue();
        }
    }
}
