package org.embulk.input.kintone;

//import com.cybozu.kintone.client.model.file.FileModel;
import com.kintone.client.model.FileBody;
import com.kintone.client.model.User;
//import com.kintone.client.model.app.field.FileFieldProperty;
//import com.kintone.client.model.record.FileFieldValue;
import com.kintone.client.model.record.Record;
import com.kintone.client.model.record.TableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import com.cybozu.kintone.client.model.record.field.FieldValue;
//import com.cybozu.kintone.client.model.member.Member;
import com.google.gson.Gson;

//import java.util.ArrayList;
//import java.util.HashMap;
import java.util.List;

public class KintoneAccessor {
    private final Logger logger = LoggerFactory.getLogger(KintoneAccessor.class);
    private final Gson gson = new Gson();

//    private final HashMap<String, FieldValue> record;
    private final Record record;
    private final String delimiter = "\n";

    public KintoneAccessor(final Record record) {
        this.record = record;
    }

    public String get(String name) {
        if (name.equals("$id")) {
            return record.getId().toString();
        }
        if (name.equals("$revision")) {
            return record.getRevision().toString();
        }
        switch (this.record.getFieldType(name)) {
            case USER_SELECT:
                List<User> users1 = this.record.getStatusAssigneeFieldValue();
                return usersToString(users1);
            case ORGANIZATION_SELECT:
                List<User> users2 = this.record.getStatusAssigneeFieldValue();
                return usersToString(users2);
            case GROUP_SELECT:
                List<User> users3 = this.record.getStatusAssigneeFieldValue();
                return usersToString(users3);
            case STATUS_ASSIGNEE:
//                ArrayList<Member> members = (ArrayList<Member>) this.record.get(name).getValue();
//                return members.stream().map(Member::getCode)
//                        .reduce((accum, value) -> accum + this.delimiter + value)
//                        .orElse("");
                List<User> users4 = this.record.getStatusAssigneeFieldValue();
                return usersToString(users4);

            case SUBTABLE:
//                Object subTableValueItem = this.record.get(name).getValue();
//                return gson.toJson(subTableValueItem);
//                List<TableRow> subTableValueItem = this.record.getSubtableFieldValue(name); // TODO: weida check here
//                return gson.toJson(subTableValueItem); // TODO: weida test here
                return "weida json test";
            case CREATOR:
                User creator = record.getCreatorFieldValue();
                return creator.getCode();
            case MODIFIER:
                User user = record.getModifierFieldValue();
                return user.getCode();
//                Member m = (Member) this.record.get(name).getValue();
//                return m.getCode();
            case CHECK_BOX:
                List<String> list1 = this.record.getCheckBoxFieldValue(name);
                return ItemListToString(list1);
            case MULTI_SELECT:
                List<String> list2 = this.record.getMultiSelectFieldValue(name);
                return ItemListToString(list2);
            case CATEGORY:
//                ArrayList<String> selectedItemList = (ArrayList<String>) this.record.get(name).getValue();
                List<String> list3 = this.record.getCategoryFieldValue();
                return ItemListToString(list3);
            case FILE:
//                ArrayList<FileModel> cbFileList = (ArrayList<FileModel>) this.record.get(name).getValue(); // TODO: weida revert here
                List<FileBody> cbFileList = this.record.getFileFieldValue(name);
                return cbFileList.stream().map(FileBody::getFileKey)
                        .reduce((accum, value) -> accum + this.delimiter + value)
                        .orElse("");
            case NUMBER:
//                return String.valueOf(this.record.get(name).getValue());
                return String.valueOf(this.record.getNumberFieldValue(name));
            default:
//                return (String) this.record.get(name).getValue();
//                return this.record.getFieldValue(name).toString(); // TODO: weida check if this works expectedly as values are authentically converted to string
                return "weida other test";
        }
    }

    private String ItemListToString(List<String> list) {
        return list.stream()
                .reduce((accum, value) -> accum + this.delimiter + value)
                .orElse("");
    }

    private String usersToString(List<User> list) {
        return list.stream().map(User::getCode)
                .reduce((accum, value) -> accum + this.delimiter + value)
                .orElse("");
    }
}
