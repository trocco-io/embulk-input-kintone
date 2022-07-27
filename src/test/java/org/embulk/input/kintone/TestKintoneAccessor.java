package org.embulk.input.kintone;

import com.kintone.client.model.Group;
import com.kintone.client.model.Organization;
import com.kintone.client.model.User;
import com.kintone.client.model.record.CheckBoxFieldValue;
import com.kintone.client.model.record.DateFieldValue;
import com.kintone.client.model.record.DateTimeFieldValue;
import com.kintone.client.model.record.DropDownFieldValue;
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
import com.kintone.client.model.record.SubtableFieldValue;
import com.kintone.client.model.record.TableRow;
import com.kintone.client.model.record.TimeFieldValue;
import com.kintone.client.model.record.UserSelectFieldValue;
import org.junit.Test;

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
    private Integer uniqueKey = 1;

    public Record createTestRecord()
    {
        Record tRecord = new Record();

        tRecord.putField("文字列__1行", new SingleLineTextFieldValue("test single text"));
        tRecord.putField("数値", new NumberFieldValue(this.uniqueKey.longValue()));
        this.uniqueKey += 1;
        tRecord.putField("文字列__複数行", new MultiLineTextFieldValue("test multi text"));
        tRecord.putField("リッチエディター", new RichTextFieldValue("<div>test rich text<br /></div>"));

        ArrayList<String> selectedItemList = new ArrayList<>();
        selectedItemList.add("sample1");
        selectedItemList.add("sample2");
        tRecord.putField("チェックボックス", new CheckBoxFieldValue(selectedItemList));
        tRecord.putField("ラジオボタン", new RadioButtonFieldValue("sample2"));
        tRecord.putField("ドロップダウン", new DropDownFieldValue("sample3"));
        tRecord.putField("複数選択", new MultiSelectFieldValue(selectedItemList));
        tRecord.putField("リンク", new LinkFieldValue("https://cybozu.co.jp/"));
        tRecord.putField("日付", new DateFieldValue(LocalDate.of(2018, 1, 1)));
        tRecord.putField("時刻", new TimeFieldValue(LocalTime.of(12, 34)));
        tRecord.putField("日時", new DateTimeFieldValue(ZonedDateTime.parse("2018-01-02T02:30:00Z")));

        ArrayList<User> userList = new ArrayList<>();
        userList.add(testman1);
        userList.add(testman2);
        tRecord.putField("ユーザー選択", new UserSelectFieldValue(userList));
        ArrayList<Group> groupList = new ArrayList<>();
        groupList.add(testgroup1);
        groupList.add(testgroup2);
        tRecord.putField("グループ選択", new GroupSelectFieldValue(groupList));
        ArrayList<Organization> orgList = new ArrayList<>();
        orgList.add(testorg1);
        orgList.add(testorg2);
        tRecord.putField("組織選択", new OrganizationSelectFieldValue(orgList));

        TableRow tableItem1 = new TableRow(1L);
        tableItem1.putField("sample field1", new SingleLineTextFieldValue("sample_text1"));
        SubtableFieldValue subTableRecords = new SubtableFieldValue(tableItem1);
        tRecord.putField("サブテーブル", subTableRecords);

        return tRecord;
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
