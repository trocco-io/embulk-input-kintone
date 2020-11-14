package org.embulk.input.kintone;


import com.cybozu.kintone.client.model.app.form.FieldType;
import com.cybozu.kintone.client.model.record.field.FieldValue;
import com.cybozu.kintone.client.model.record.SubTableValueItem;
import com.cybozu.kintone.client.model.member.Member;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.Assert.*;


public class TestKintoneAccessor {
    private static Member testman1 = new Member("code1", "name1");
    private static Member testman2 = new Member("code2", "name2");
    private static Member testgroup1 = new Member("code3", "name3");
    private static Member testgroup2 = new Member("code4", "name4");
    private static Member testorg1 = new Member("code5", "name5");
    private static Member testorg2 = new Member("code6", "name6");
    private Integer uniqueKey = 1;

    public HashMap<String, FieldValue> createTestRecord() {
        HashMap<String, FieldValue> testRecord = new HashMap<>();

        TestHelper.addField(testRecord, "文字列__1行", FieldType.SINGLE_LINE_TEXT, "test single text");
        TestHelper.addField(testRecord, "数値", FieldType.NUMBER, this.uniqueKey);
        this.uniqueKey += 1;
        TestHelper.addField(testRecord, "文字列__複数行", FieldType.MULTI_LINE_TEXT, "test multi text");
        TestHelper.addField(testRecord, "リッチエディター", FieldType.RICH_TEXT, "<div>test rich text<br /></div>");

        ArrayList<String> selectedItemList = new ArrayList<>();
        selectedItemList.add("sample1");
        selectedItemList.add("sample2");
        TestHelper.addField(testRecord, "チェックボックス", FieldType.CHECK_BOX, selectedItemList);
        TestHelper.addField(testRecord, "ラジオボタン", FieldType.RADIO_BUTTON, "sample2");
        TestHelper.addField(testRecord, "ドロップダウン", FieldType.DROP_DOWN, "sample3");
        TestHelper.addField(testRecord, "複数選択", FieldType.MULTI_SELECT, selectedItemList);
        TestHelper.addField(testRecord, "リンク", FieldType.LINK, "http://cybozu.co.jp/");
        TestHelper.addField(testRecord, "日付", FieldType.DATE, "2018-01-01");
        TestHelper.addField(testRecord, "時刻", FieldType.TIME, "12:34");
        TestHelper.addField(testRecord, "日時", FieldType.DATETIME, "2018-01-02T02:30:00Z");

        ArrayList<Member> userList = new ArrayList<>();
        userList.add(testman1);
        userList.add(testman2);
        TestHelper.addField(testRecord, "ユーザー選択", FieldType.USER_SELECT, userList);
        ArrayList<Member> groupList = new ArrayList<>();
        groupList.add(testgroup1);
        groupList.add(testgroup2);
        TestHelper.addField(testRecord, "グループ選択", FieldType.GROUP_SELECT, groupList);
        ArrayList<Member> orgList = new ArrayList<>();
        orgList.add(testorg1);
        orgList.add(testorg2);
        TestHelper.addField(testRecord, "組織選択", FieldType.ORGANIZATION_SELECT, orgList);

        SubTableValueItem tableItem1 = new SubTableValueItem();
        tableItem1.setID(1);
        HashMap<String, FieldValue> tableItemValue1 = new HashMap<>();
        FieldValue fv1 = new FieldValue();
        fv1.setType(FieldType.SINGLE_LINE_TEXT);
        fv1.setValue("sample_text1");
        tableItemValue1.put("sample field1", fv1);
        tableItem1.setValue(tableItemValue1);
        ArrayList<SubTableValueItem> subTableRecords = new ArrayList<>();
        subTableRecords.add(tableItem1);
        TestHelper.addField(testRecord, "サブテーブル", FieldType.SUBTABLE, subTableRecords);

        return testRecord;
    }

    @Test
    public void testAccess() {
        HashMap<String, FieldValue> testRecord = createTestRecord();
        KintoneAccessor accessor = new KintoneAccessor(testRecord);
        String multiValue = "sample1\nsample2";
        String userSelect = "code1\ncode2";
        String groupSelect = "code3\ncode4";
        String orgSelect = "code5\ncode6";
        String subTableValue = "[{\"id\":1,\"value\":{\"sample field1\":{\"type\":\"SINGLE_LINE_TEXT\",\"value\":\"sample_text1\"}}}]";
        assertEquals(testRecord.get("文字列__1行").getValue(), accessor.get("文字列__1行"));
        assertEquals("1", accessor.get("数値"));
        assertEquals(testRecord.get("文字列__複数行").getValue(), accessor.get("文字列__複数行"));
        assertEquals(testRecord.get("リッチエディター").getValue(), accessor.get("リッチエディター"));
        assertEquals(multiValue, accessor.get("チェックボックス"));
        assertEquals(testRecord.get("ラジオボタン").getValue(), accessor.get("ラジオボタン"));
        assertEquals(testRecord.get("ドロップダウン").getValue(), accessor.get("ドロップダウン"));
        assertEquals(multiValue, accessor.get("複数選択"));
        assertEquals(testRecord.get("リンク").getValue(), accessor.get("リンク"));
        assertEquals(testRecord.get("日付").getValue(), accessor.get("日付"));
        assertEquals(testRecord.get("時刻").getValue(), accessor.get("時刻"));
        assertEquals(testRecord.get("日時").getValue(), accessor.get("日時"));
        assertEquals(userSelect, accessor.get("ユーザー選択"));
        assertEquals(groupSelect, accessor.get("グループ選択"));
        assertEquals(orgSelect, accessor.get("組織選択"));
        assertEquals(subTableValue, accessor.get("サブテーブル"));
    }
}
