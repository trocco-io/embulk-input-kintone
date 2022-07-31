package org.embulk.input.kintone;

import com.kintone.client.model.FileBody;
import com.kintone.client.model.Group;
import com.kintone.client.model.Organization;
import com.kintone.client.model.User;
import com.kintone.client.model.record.CalcFieldValue;
import com.kintone.client.model.record.CategoryFieldValue;
import com.kintone.client.model.record.CheckBoxFieldValue;
import com.kintone.client.model.record.CreatedTimeFieldValue;
import com.kintone.client.model.record.CreatorFieldValue;
import com.kintone.client.model.record.DateFieldValue;
import com.kintone.client.model.record.DateTimeFieldValue;
import com.kintone.client.model.record.DropDownFieldValue;
import com.kintone.client.model.record.FileFieldValue;
import com.kintone.client.model.record.GroupSelectFieldValue;
import com.kintone.client.model.record.LinkFieldValue;
import com.kintone.client.model.record.ModifierFieldValue;
import com.kintone.client.model.record.MultiLineTextFieldValue;
import com.kintone.client.model.record.MultiSelectFieldValue;
import com.kintone.client.model.record.NumberFieldValue;
import com.kintone.client.model.record.OrganizationSelectFieldValue;
import com.kintone.client.model.record.RadioButtonFieldValue;
import com.kintone.client.model.record.Record;
import com.kintone.client.model.record.RecordNumberFieldValue;
import com.kintone.client.model.record.RichTextFieldValue;
import com.kintone.client.model.record.SingleLineTextFieldValue;
import com.kintone.client.model.record.StatusAssigneeFieldValue;
import com.kintone.client.model.record.StatusFieldValue;
import com.kintone.client.model.record.SubtableFieldValue;
import com.kintone.client.model.record.TableRow;
import com.kintone.client.model.record.TimeFieldValue;
import com.kintone.client.model.record.UpdatedTimeFieldValue;
import com.kintone.client.model.record.UserSelectFieldValue;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class TestKintoneAccessor
{
    private static final User testman1 = new User("name1", "code1");
    private static final User testman2 = new User("name2", "code2");
    private static final Group testgroup1 = new Group("name3", "code3");
    private static final Group testgroup2 = new Group("name4", "code4");
    private static final Organization testorg1 = new Organization("name5", "code5");
    private static final Organization testorg2 = new Organization("name5", "code6");
    private static final User testassignee1 = new User("name7", "code7");
    private static final User testassignee2 = new User("name8", "code8");
    private static final User creator = new User("name9", "code9");
    private static final User modifier = new User("name10", "code10");
    private Integer uniqueKey = 1;

    public Record createTestRecord()
    {
        Record testRecord = new Record(123L, 456L);

        testRecord.putField("文字列__1行", new SingleLineTextFieldValue("test single text"));
        testRecord.putField("数値", new NumberFieldValue(this.uniqueKey.longValue()));
        this.uniqueKey += 1;
        testRecord.putField("文字列__複数行", new MultiLineTextFieldValue("test multi text"));
        testRecord.putField("リッチエディター", new RichTextFieldValue("<div>test rich text<br /></div>"));

        ArrayList<String> selectedItemList = new ArrayList<>();
        selectedItemList.add("sample1");
        selectedItemList.add("sample2");
        testRecord.putField("チェックボックス", new CheckBoxFieldValue(selectedItemList));
        testRecord.putField("ラジオボタン", new RadioButtonFieldValue("sample2"));
        testRecord.putField("ドロップダウン", new DropDownFieldValue("sample3"));
        testRecord.putField("複数選択", new MultiSelectFieldValue(selectedItemList));
        testRecord.putField("リンク", new LinkFieldValue("https://cybozu.co.jp/"));
        testRecord.putField("日付", new DateFieldValue(LocalDate.of(2018, 1, 1)));
        testRecord.putField("時刻", new TimeFieldValue(LocalTime.of(12, 34)));
        testRecord.putField("日時", new DateTimeFieldValue(ZonedDateTime.parse("2018-01-02T02:30:00Z")));

        ArrayList<User> userList = new ArrayList<>();
        userList.add(testman1);
        userList.add(testman2);
        testRecord.putField("ユーザー選択", new UserSelectFieldValue(userList));
        ArrayList<Group> groupList = new ArrayList<>();
        groupList.add(testgroup1);
        groupList.add(testgroup2);
        testRecord.putField("グループ選択", new GroupSelectFieldValue(groupList));
        ArrayList<Organization> orgList = new ArrayList<>();
        orgList.add(testorg1);
        orgList.add(testorg2);
        testRecord.putField("組織選択", new OrganizationSelectFieldValue(orgList));
        ArrayList<User> assigneeList = new ArrayList<>();
        assigneeList.add(testassignee1);
        assigneeList.add(testassignee2);
        testRecord.putField("作業者", new StatusAssigneeFieldValue(assigneeList));

        TableRow tableItem1 = new TableRow(1L);
        tableItem1.putField("sample field1", new SingleLineTextFieldValue("sample_text1"));
        SubtableFieldValue subTableRecords = new SubtableFieldValue(tableItem1);
        testRecord.putField("サブテーブル", subTableRecords);

        testRecord.putField("レコード番号", new RecordNumberFieldValue("sample_record_number"));
        testRecord.putField("作成者", new CreatorFieldValue(creator));
        testRecord.putField("作成日時", new CreatedTimeFieldValue(ZonedDateTime.parse("2012-01-11T11:30:00Z")));
        testRecord.putField("更新者", new ModifierFieldValue(modifier));
        testRecord.putField("更新日時", new UpdatedTimeFieldValue(ZonedDateTime.parse("2012-01-11T11:30:00Z")));
        testRecord.putField("計算", new CalcFieldValue(new BigDecimal("1.23E-12")));
        FileBody body1 = new FileBody();
        body1.setFileKey("sample_file1");
        FileBody body2 = new FileBody();
        body2.setFileKey("sample_file2");
        testRecord.putField("添付ファイル", new FileFieldValue(body1, body2));
        testRecord.putField("カテゴリー", new CategoryFieldValue("sample_category1", "sample_category2"));
        testRecord.putField("ステータス", new StatusFieldValue("sample_status"));

        return testRecord;
    }

    @Test
    public void testAccess()
    {
        Record testRecord = createTestRecord();
        KintoneAccessor accessor = new KintoneAccessor(testRecord);
        String multiValue = "sample1\nsample2";
        String userSelect = "code1\ncode2";
        String groupSelect = "code3\ncode4";
        String orgSelect = "code5\ncode6";
        String assigneeSelect = "code7\ncode8";
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
        assertEquals("2018-01-01", accessor.get("日付"));
        assertEquals("12:34", accessor.get("時刻"));
        assertEquals("2018-01-02T02:30:00Z", accessor.get("日時"));
        assertEquals(userSelect, accessor.get("ユーザー選択"));
        assertEquals(groupSelect, accessor.get("グループ選択"));
        assertEquals(orgSelect, accessor.get("組織選択"));
        assertEquals(assigneeSelect, accessor.get("作業者"));
        assertEquals(subTableValue, accessor.get("サブテーブル"));
        assertEquals("sample_record_number", accessor.get("レコード番号"));
        assertEquals("123", accessor.get("$id"));
        assertEquals("456", accessor.get("$revision"));
        assertEquals("code9", accessor.get("作成者"));
        assertEquals("2012-01-11T11:30:00Z", accessor.get("作成日時"));
        assertEquals("code10", accessor.get("更新者"));
        assertEquals("2012-01-11T11:30:00Z", accessor.get("更新日時"));
        assertEquals("1.23E-12", accessor.get("計算"));
        assertEquals("sample_file1\nsample_file2", accessor.get("添付ファイル"));
        assertEquals("sample_category1\nsample_category2", accessor.get("カテゴリー"));
        assertEquals("sample_status", accessor.get("ステータス"));
    }
}
