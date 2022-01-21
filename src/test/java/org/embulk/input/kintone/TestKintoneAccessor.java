package org.embulk.input.kintone;


//import com.cybozu.kintone.client.model.app.form.FieldType;
//import com.cybozu.kintone.client.model.record.field.FieldValue;
//import com.cybozu.kintone.client.model.record.SubTableValueItem;
//import com.cybozu.kintone.client.model.member.Member;
import com.kintone.client.model.Group;
import com.kintone.client.model.Organization;
import com.kintone.client.model.User;
import com.kintone.client.model.record.*;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.*;


public class TestKintoneAccessor {
    private static User testman1 = new User("name1", "code1");
    private static User testman2 = new User("name2", "code2");
    private static Group testgroup1 = new Group("name3", "code3");
    private static Group testgroup2 = new Group("name4", "code4");
    private static Organization testorg1 = new Organization("name5", "code5");
    private static Organization testorg2 = new Organization("name5", "code6");
    private Integer uniqueKey = 1;

//    public HashMap<String, FieldValue> createTestRecord() {
    public Record createTestRecord() {
        Record tRecord = new Record();
        HashMap<String, FieldValue> testRecord = new HashMap<>();

        tRecord.putField("文字列__1行", new SingleLineTextFieldValue("test single text"));
//        TestHelper.addField(testRecord, "文字列__1行", FieldType.SINGLE_LINE_TEXT, "test single text");
        tRecord.putField("数値", new NumberFieldValue(this.uniqueKey.longValue()));
//        TestHelper.addField(testRecord, "数値", FieldType.NUMBER, this.uniqueKey);
        this.uniqueKey += 1;
        tRecord.putField("文字列__複数行", new MultiLineTextFieldValue("test multi text"));
//        TestHelper.addField(testRecord, "文字列__複数行", FieldType.MULTI_LINE_TEXT, "test multi text");
        tRecord.putField("リッチエディター", new RichTextFieldValue("<div>test rich text<br /></div>"));
//        TestHelper.addField(testRecord, "リッチエディター", FieldType.RICH_TEXT, "<div>test rich text<br /></div>");

        ArrayList<String> selectedItemList = new ArrayList<>();
        selectedItemList.add("sample1");
        selectedItemList.add("sample2");
        tRecord.putField("チェックボックス", new CheckBoxFieldValue(selectedItemList));
//        TestHelper.addField(testRecord, "チェックボックス", FieldType.CHECK_BOX, selectedItemList);
        tRecord.putField("ラジオボタン", new RadioButtonFieldValue("sample2"));
//        TestHelper.addField(testRecord, "ラジオボタン", FieldType.RADIO_BUTTON, "sample2");
        tRecord.putField("ドロップダウン", new DropDownFieldValue("sample3"));
//        TestHelper.addField(testRecord, "ドロップダウン", FieldType.DROP_DOWN, "sample3");
        tRecord.putField("複数選択", new MultiSelectFieldValue(selectedItemList));
//        TestHelper.addField(testRecord, "複数選択", FieldType.MULTI_SELECT, selectedItemList);
        tRecord.putField("リンク", new LinkFieldValue("http://cybozu.co.jp/"));
//        TestHelper.addField(testRecord, "リンク", FieldType.LINK, "http://cybozu.co.jp/");
        tRecord.putField("日付", new DateFieldValue(LocalDate.of(2018, 1, 1)));
//        TestHelper.addField(testRecord, "日付", FieldType.DATE, "2018-01-01");
        tRecord.putField("時刻", new TimeFieldValue(LocalTime.of(12, 34)));
//        TestHelper.addField(testRecord, "時刻", FieldType.TIME, "12:34");
        tRecord.putField("日時", new DateTimeFieldValue(ZonedDateTime.parse("2018-01-02T02:30:00Z")));
//        TestHelper.addField(testRecord, "日時", FieldType.DATETIME, "2018-01-02T02:30:00Z");

        ArrayList<User> userList = new ArrayList<>();
        userList.add(testman1);
        userList.add(testman2);
        tRecord.putField("ユーザー選択", new UserSelectFieldValue(userList));
//        TestHelper.addField(testRecord, "ユーザー選択", FieldType.USER_SELECT, userList);
        ArrayList<Group> groupList = new ArrayList<>();
        groupList.add(testgroup1);
        groupList.add(testgroup2);
        tRecord.putField("グループ選択", new GroupSelectFieldValue(groupList));
//        TestHelper.addField(testRecord, "グループ選択", FieldType.GROUP_SELECT, groupList);
        ArrayList<Organization> orgList = new ArrayList<>();
        orgList.add(testorg1);
        orgList.add(testorg2);
        tRecord.putField("組織選択", new OrganizationSelectFieldValue(orgList));
//        TestHelper.addField(testRecord, "組織選択", FieldType.ORGANIZATION_SELECT, orgList);

//        SubTableValueItem tableItem1 = new SubTableValueItem();
        TableRow tableItem1 = new TableRow(Long.valueOf(1));
//        tableItem1.setID(1);
//        HashMap<String, FieldValue> tableItemValue1 = new HashMap<>();
//        FieldValue fv1 = new FieldValue();
//        fv1.setType(FieldType.SINGLE_LINE_TEXT);
//        fv1.setValue("sample_text1");
//                       SingleLineTextFieldValue fv1 = new SingleLineTextFieldValue("sample_text1");
//        tableItemValue1.put("sample field1", fv1);
//        tableItem1.setValue(tableItemValue1);
        tableItem1.putField("sample field1", new SingleLineTextFieldValue("sample_text1"));
        SubtableFieldValue subTableRecords = new SubtableFieldValue(tableItem1);
//        ArrayList<SubTableValueItem> subTableRecords = new ArrayList<>();
//        subTableRecords.add(tableItem1);
        tRecord.putField("サブテーブル", subTableRecords);
//        TestHelper.addField(testRecord, "サブテーブル", FieldType.SUBTABLE, subTableRecords);
//
//        return testRecord;
        return tRecord;
    }

    @Test
    public void testAccess() {
//        HashMap<String, FieldValue> testRecord = createTestRecord();
//        KintoneAccessor accessor = new KintoneAccessor(testRecord);
//        String multiValue = "sample1\nsample2";
//        String userSelect = "code1\ncode2";
//        String groupSelect = "code3\ncode4";
//        String orgSelect = "code5\ncode6";
//        String subTableValue = "[{\"id\":1,\"value\":{\"sample field1\":{\"type\":\"SINGLE_LINE_TEXT\",\"value\":\"sample_text1\"}}}]";
//        assertEquals(testRecord.get("文字列__1行").getValue(), accessor.get("文字列__1行"));
//        assertEquals("1", accessor.get("数値"));
//        assertEquals(testRecord.get("文字列__複数行").getValue(), accessor.get("文字列__複数行"));
//        assertEquals(testRecord.get("リッチエディター").getValue(), accessor.get("リッチエディター"));
//        assertEquals(multiValue, accessor.get("チェックボックス"));
//        assertEquals(testRecord.get("ラジオボタン").getValue(), accessor.get("ラジオボタン"));
//        assertEquals(testRecord.get("ドロップダウン").getValue(), accessor.get("ドロップダウン"));
//        assertEquals(multiValue, accessor.get("複数選択"));
//        assertEquals(testRecord.get("リンク").getValue(), accessor.get("リンク"));
//        assertEquals(testRecord.get("日付").getValue(), accessor.get("日付"));
//        assertEquals(testRecord.get("時刻").getValue(), accessor.get("時刻"));
//        assertEquals(testRecord.get("日時").getValue(), accessor.get("日時"));
//        assertEquals(userSelect, accessor.get("ユーザー選択"));
//        assertEquals(groupSelect, accessor.get("グループ選択"));
//        assertEquals(orgSelect, accessor.get("組織選択"));
//        assertEquals(subTableValue, accessor.get("サブテーブル"));
        Record testRecord = createTestRecord();
        KintoneAccessor accessor = new KintoneAccessor(testRecord);
        String multiValue = "sample1\nsample2";
        String userSelect = "code1\ncode2";
        String groupSelect = "code3\ncode4";
        String orgSelect = "code5\ncode6";
        String subTableValue = "[{\"fields\":{\"sample field1\":{\"value\":\"sample_text1\"}},\"id\":1}]";
        assertEquals(testRecord.getSingleLineTextFieldValue("文字列__1行"), accessor.get("文字列__1行"));
        assertEquals("1", accessor.get("数値"));
        assertEquals(testRecord.getMultiLineTextFieldValue("文字列__複数行"), accessor.get("文字列__複数行"));
        assertEquals(testRecord.getRichTextFieldValue("リッチエディター"), accessor.get("リッチエディター"));
        assertEquals(multiValue, accessor.get("チェックボックス"));
        assertEquals(testRecord.getRadioButtonFieldValue("ラジオボタン"), accessor.get("ラジオボタン"));
        assertEquals(testRecord.getDropDownFieldValue("ドロップダウン"), accessor.get("ドロップダウン"));
        assertEquals(multiValue, accessor.get("複数選択"));
        assertEquals(testRecord.getLinkFieldValue("リンク"), accessor.get("リンク"));
        assertEquals(testRecord.getDateFieldValue("日付").toString(), accessor.get("日付"));
        assertEquals(testRecord.getTimeFieldValue("時刻").toString(), accessor.get("時刻"));
        assertEquals(testRecord.getDateTimeFieldValue("日時").toString(), accessor.get("日時"));
        assertEquals(userSelect, accessor.get("ユーザー選択"));
        assertEquals(groupSelect, accessor.get("グループ選択"));
        assertEquals(orgSelect, accessor.get("組織選択"));
        assertEquals(subTableValue, accessor.get("サブテーブル"));
    }

}
