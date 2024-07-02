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
import static org.junit.Assert.assertNull;

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
        testRecord.putField("計算(Calc)", new CalcFieldValue("1.23E-12"));
        testRecord.putField("数値(Calc)", new CalcFieldValue("1234"));
        testRecord.putField("日時(Calc)", new CalcFieldValue("2012-01-11T11:30:00Z"));
        testRecord.putField("日付(Calc)", new CalcFieldValue("2012-01-11"));
        testRecord.putField("時刻(Calc)", new CalcFieldValue("11:30"));
        testRecord.putField("時間(Calc)", new CalcFieldValue("49:30"));
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
        String subTableValue = "[{\"id\":1,\"value\":{\"sample field1\":{\"type\":\"SINGLE_LINE_TEXT\",\"value\":\"sample_text1\"}}}]";
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
        assertEquals("1.23E-12", accessor.get("計算(Calc)"));
        assertEquals("1234", accessor.get("数値(Calc)"));
        assertEquals("2012-01-11T11:30:00Z", accessor.get("日時(Calc)"));
        assertEquals("2012-01-11", accessor.get("日付(Calc)"));
        assertEquals("11:30", accessor.get("時刻(Calc)"));
        assertEquals("49:30", accessor.get("時間(Calc)"));
        assertEquals("sample_file1\nsample_file2", accessor.get("添付ファイル"));
        assertEquals("sample_category1\nsample_category2", accessor.get("カテゴリー"));
        assertEquals("sample_status", accessor.get("ステータス"));
    }

    @Test
    public void testFields()
    {
        final KintoneAccessor accessor = new KintoneAccessor(record());
        assertEquals("APPCODE-1", accessor.get("レコード番号"));
        assertEquals("1", accessor.get("$id"));
        assertEquals("5", accessor.get("$revision"));
        assertEquals("sato", accessor.get("作成者"));
        assertEquals("2021-01-11T11:11:11Z", accessor.get("作成日時"));
        assertEquals("guest/kato@cybozu.com", accessor.get("更新者"));
        assertEquals("2022-02-22T22:22:22Z", accessor.get("更新日時"));
        assertEquals("テストです。", accessor.get("文字列（1行）"));
        assertEquals("テスト\nです。", accessor.get("文字列（複数行）"));
        assertEquals("<a href=\"https://www.cybozu.com\">サイボウズ</a>", accessor.get("リッチエディター"));
        assertEquals("123", accessor.get("数値"));
        assertEquals("456", accessor.get("計算"));
        assertEquals("選択肢1\n選択肢2", accessor.get("チェックボックス"));
        assertEquals("選択肢3", accessor.get("ラジオボタン"));
        assertEquals("選択肢4\n選択肢5", accessor.get("複数選択"));
        assertEquals("選択肢6", accessor.get("ドロップダウン"));
        assertEquals("guest/sato@cybozu.com\nkato", accessor.get("ユーザー選択"));
        assertEquals("kaihatsu\njinji", accessor.get("組織選択"));
        assertEquals("project_manager\nteam_leader", accessor.get("グループ選択"));
        assertEquals("2012-01-11", accessor.get("日付"));
        assertEquals("11:30", accessor.get("時刻"));
        assertEquals("2012-01-11T11:30:00Z", accessor.get("日時"));
        assertEquals("https://cybozu.co.jp/", accessor.get("リンク"));
        assertEquals("201202061155587E339F9067544F1A92C743460E3D12B3297\n201202061155583C763E30196F419E83E91D2E4A03746C273", accessor.get("添付ファイル"));
        assertEquals("[{\"id\":48290,\"value\":{\"リッチエディター\":{\"type\":\"RICH_TEXT\",\"value\":\"\\u003ca href\\u003d\\\"https://www.cybozu.com\\\"\\u003eサイボウズ\\u003c/a\\u003e\"},\"グループ選択\":{\"type\":\"GROUP_SELECT\",\"value\":[{\"name\":\"プロジェクトマネージャー\",\"code\":\"project_manager\"},{\"name\":\"チームリーダー\",\"code\":\"team_leader\"}]},\"文字列（1行）\":{\"type\":\"SINGLE_LINE_TEXT\",\"value\":\"テストです。\"},\"ラジオボタン\":{\"type\":\"RADIO_BUTTON\",\"value\":\"選択肢3\"},\"ドロップダウン\":{\"type\":\"DROP_DOWN\",\"value\":\"選択肢6\"},\"組織選択\":{\"type\":\"ORGANIZATION_SELECT\",\"value\":[{\"name\":\"開発部\",\"code\":\"kaihatsu\"},{\"name\":\"人事部\",\"code\":\"jinji\"}]},\"ユーザー選択\":{\"type\":\"USER_SELECT\",\"value\":[{\"name\":\"Noboru Sato\",\"code\":\"guest/sato@cybozu.com\"},{\"name\":\"Misaki Kato\",\"code\":\"kato\"}]},\"日時\":{\"type\":\"DATETIME\",\"value\":\"2012-01-11T11:30:00Z\"},\"文字列（複数行）\":{\"type\":\"MULTI_LINE_TEXT\",\"value\":\"テスト\\nです。\"},\"時刻\":{\"type\":\"TIME\",\"value\":\"11:30\"},\"チェックボックス\":{\"type\":\"CHECK_BOX\",\"value\":[\"選択肢1\",\"選択肢2\"]},\"複数選択\":{\"type\":\"MULTI_SELECT\",\"value\":[\"選択肢4\",\"選択肢5\"]},\"数値\":{\"type\":\"NUMBER\",\"value\":\"123\"},\"添付ファイル\":{\"type\":\"FILE\",\"value\":[{\"contentType\":\"text/plain\",\"fileKey\":\"201202061155587E339F9067544F1A92C743460E3D12B3297\",\"name\":\"17to20_VerupLog (1).txt\",\"size\":\"23175\"},{\"contentType\":\"application/json\",\"fileKey\":\"201202061155583C763E30196F419E83E91D2E4A03746C273\",\"name\":\"17to20_VerupLog.txt\",\"size\":\"23176\"}]},\"リンク\":{\"type\":\"LINK\",\"value\":\"https://cybozu.co.jp/\"},\"計算\":{\"type\":\"CALC\",\"value\":\"456\"},\"日付\":{\"type\":\"DATE\",\"value\":\"2012-01-11\"}}},{\"id\":48291,\"value\":{\"リッチエディター\":{\"type\":\"RICH_TEXT\",\"value\":\"\\u003ca href\\u003d\\\"https://www.cybozu.com\\\"\\u003eサイボウズ\\u003c/a\\u003e\"},\"グループ選択\":{\"type\":\"GROUP_SELECT\",\"value\":[{\"name\":\"プロジェクトマネージャー\",\"code\":\"project_manager\"},{\"name\":\"チームリーダー\",\"code\":\"team_leader\"}]},\"文字列（1行）\":{\"type\":\"SINGLE_LINE_TEXT\",\"value\":\"テストです。\"},\"ラジオボタン\":{\"type\":\"RADIO_BUTTON\",\"value\":\"選択肢3\"},\"ドロップダウン\":{\"type\":\"DROP_DOWN\",\"value\":\"選択肢6\"},\"組織選択\":{\"type\":\"ORGANIZATION_SELECT\",\"value\":[{\"name\":\"開発部\",\"code\":\"kaihatsu\"},{\"name\":\"人事部\",\"code\":\"jinji\"}]},\"ユーザー選択\":{\"type\":\"USER_SELECT\",\"value\":[{\"name\":\"Noboru Sato\",\"code\":\"guest/sato@cybozu.com\"},{\"name\":\"Misaki Kato\",\"code\":\"kato\"}]},\"日時\":{\"type\":\"DATETIME\",\"value\":\"2012-01-11T11:30:00Z\"},\"文字列（複数行）\":{\"type\":\"MULTI_LINE_TEXT\",\"value\":\"テスト\\nです。\"},\"時刻\":{\"type\":\"TIME\",\"value\":\"11:30\"},\"チェックボックス\":{\"type\":\"CHECK_BOX\",\"value\":[\"選択肢1\",\"選択肢2\"]},\"複数選択\":{\"type\":\"MULTI_SELECT\",\"value\":[\"選択肢4\",\"選択肢5\"]},\"数値\":{\"type\":\"NUMBER\",\"value\":\"123\"},\"添付ファイル\":{\"type\":\"FILE\",\"value\":[{\"contentType\":\"text/plain\",\"fileKey\":\"201202061155587E339F9067544F1A92C743460E3D12B3297\",\"name\":\"17to20_VerupLog (1).txt\",\"size\":\"23175\"},{\"contentType\":\"application/json\",\"fileKey\":\"201202061155583C763E30196F419E83E91D2E4A03746C273\",\"name\":\"17to20_VerupLog.txt\",\"size\":\"23176\"}]},\"リンク\":{\"type\":\"LINK\",\"value\":\"https://cybozu.co.jp/\"},\"計算\":{\"type\":\"CALC\",\"value\":\"456\"},\"日付\":{\"type\":\"DATE\",\"value\":\"2012-01-11\"}}}]", accessor.get("テーブル"));
        assertEquals("category1\ncategory2", accessor.get("カテゴリー"));
        assertEquals("未処理", accessor.get("ステータス"));
        assertEquals("sato\nkato", accessor.get("作業者"));
    }

    private Record record()
    {
        final Record record = new Record(1L, 5L);
        record.putField("レコード番号", new RecordNumberFieldValue("APPCODE-1"));
        record.putField("作成者", new CreatorFieldValue(user("sato", "Noboru Sato")));
        record.putField("作成日時", new CreatedTimeFieldValue(ZonedDateTime.parse("2021-01-11T11:11:11Z")));
        record.putField("更新者", new ModifierFieldValue(user("guest/kato@cybozu.com", "Misaki Kato")));
        record.putField("更新日時", new UpdatedTimeFieldValue(ZonedDateTime.parse("2022-02-22T22:22:22Z")));
        record.putField("文字列（1行）", new SingleLineTextFieldValue("テストです。"));
        record.putField("文字列（複数行）", new MultiLineTextFieldValue("テスト\nです。"));
        record.putField("リッチエディター", new RichTextFieldValue("<a href=\"https://www.cybozu.com\">サイボウズ</a>"));
        record.putField("数値", new NumberFieldValue(new BigDecimal("123")));
        record.putField("計算", new CalcFieldValue("456"));
        record.putField("チェックボックス", new CheckBoxFieldValue("選択肢1", "選択肢2"));
        record.putField("ラジオボタン", new RadioButtonFieldValue("選択肢3"));
        record.putField("複数選択", new MultiSelectFieldValue("選択肢4", "選択肢5"));
        record.putField("ドロップダウン", new DropDownFieldValue("選択肢6"));
        record.putField("ユーザー選択", new UserSelectFieldValue(user("guest/sato@cybozu.com", "Noboru Sato"), user("kato", "Misaki Kato")));
        record.putField("組織選択", new OrganizationSelectFieldValue(organization("kaihatsu", "開発部"), organization("jinji", "人事部")));
        record.putField("グループ選択", new GroupSelectFieldValue(group("project_manager", "プロジェクトマネージャー"), group("team_leader", "チームリーダー")));
        record.putField("日付", new DateFieldValue(LocalDate.parse("2012-01-11")));
        record.putField("時刻", new TimeFieldValue(LocalTime.parse("11:30")));
        record.putField("日時", new DateTimeFieldValue(ZonedDateTime.parse("2012-01-11T11:30:00Z")));
        record.putField("リンク", new LinkFieldValue("https://cybozu.co.jp/"));
        record.putField("添付ファイル", new FileFieldValue(file("text/plain", "201202061155587E339F9067544F1A92C743460E3D12B3297", "17to20_VerupLog (1).txt", "23175"), file("application/json", "201202061155583C763E30196F419E83E91D2E4A03746C273", "17to20_VerupLog.txt", "23176")));
        record.putField("テーブル", new SubtableFieldValue(tableRow(48290L), tableRow(48291L)));
        record.putField("カテゴリー", new CategoryFieldValue("category1", "category2"));
        record.putField("ステータス", new StatusFieldValue("未処理"));
        record.putField("作業者", new StatusAssigneeFieldValue(user("sato", "Noboru Sato"), user("kato", "Misaki Kato")));
        return record;
    }

    private TableRow tableRow(final Long id)
    {
        final TableRow tableRow = new TableRow(id);
        tableRow.putField("文字列（1行）", new SingleLineTextFieldValue("テストです。"));
        tableRow.putField("文字列（複数行）", new MultiLineTextFieldValue("テスト\nです。"));
        tableRow.putField("リッチエディター", new RichTextFieldValue("<a href=\"https://www.cybozu.com\">サイボウズ</a>"));
        tableRow.putField("数値", new NumberFieldValue(new BigDecimal("123")));
        tableRow.putField("計算", new CalcFieldValue("456"));
        tableRow.putField("チェックボックス", new CheckBoxFieldValue("選択肢1", "選択肢2"));
        tableRow.putField("ラジオボタン", new RadioButtonFieldValue("選択肢3"));
        tableRow.putField("複数選択", new MultiSelectFieldValue("選択肢4", "選択肢5"));
        tableRow.putField("ドロップダウン", new DropDownFieldValue("選択肢6"));
        tableRow.putField("ユーザー選択", new UserSelectFieldValue(user("guest/sato@cybozu.com", "Noboru Sato"), user("kato", "Misaki Kato")));
        tableRow.putField("組織選択", new OrganizationSelectFieldValue(organization("kaihatsu", "開発部"), organization("jinji", "人事部")));
        tableRow.putField("グループ選択", new GroupSelectFieldValue(group("project_manager", "プロジェクトマネージャー"), group("team_leader", "チームリーダー")));
        tableRow.putField("日付", new DateFieldValue(LocalDate.parse("2012-01-11")));
        tableRow.putField("時刻", new TimeFieldValue(LocalTime.parse("11:30")));
        tableRow.putField("日時", new DateTimeFieldValue(ZonedDateTime.parse("2012-01-11T11:30:00Z")));
        tableRow.putField("リンク", new LinkFieldValue("https://cybozu.co.jp/"));
        tableRow.putField("添付ファイル", new FileFieldValue(file("text/plain", "201202061155587E339F9067544F1A92C743460E3D12B3297", "17to20_VerupLog (1).txt", "23175"), file("application/json", "201202061155583C763E30196F419E83E91D2E4A03746C273", "17to20_VerupLog.txt", "23176")));
        return tableRow;
    }

    @Test
    public void testNullFields()
    {
        final KintoneAccessor accessor = new KintoneAccessor(nullRecord());
        assertNull(accessor.get("レコード番号"));
        assertNull(accessor.get("$id"));
        assertNull(accessor.get("$revision"));
        assertNull(accessor.get("作成者"));
        assertNull(accessor.get("作成者（null項目）"));
        assertNull(accessor.get("作成日時"));
        assertNull(accessor.get("更新者"));
        assertNull(accessor.get("更新者（null項目）"));
        assertNull(accessor.get("更新日時"));
        assertNull(accessor.get("文字列（1行）"));
        assertNull(accessor.get("文字列（複数行）"));
        assertNull(accessor.get("リッチエディター"));
        assertNull(accessor.get("数値"));
        assertNull(accessor.get("計算"));
        assertEquals("", accessor.get("チェックボックス（空）"));
        assertEquals("", accessor.get("チェックボックス（null要素）"));
        assertNull(accessor.get("ラジオボタン"));
        assertEquals("", accessor.get("複数選択（空）"));
        assertEquals("", accessor.get("複数選択（null要素）"));
        assertNull(accessor.get("ドロップダウン"));
        assertEquals("", accessor.get("ユーザー選択（空）"));
        assertEquals("", accessor.get("ユーザー選択（null要素）"));
        assertEquals("", accessor.get("ユーザー選択（null項目）"));
        assertEquals("", accessor.get("組織選択（空）"));
        assertEquals("", accessor.get("組織選択（null要素）"));
        assertEquals("", accessor.get("組織選択（null項目）"));
        assertEquals("", accessor.get("グループ選択（空）"));
        assertEquals("", accessor.get("グループ選択（null要素）"));
        assertEquals("", accessor.get("グループ選択（null項目）"));
        assertNull(accessor.get("日付"));
        assertNull(accessor.get("時刻"));
        assertNull(accessor.get("日時"));
        assertNull(accessor.get("リンク"));
        assertEquals("", accessor.get("添付ファイル（空）"));
        assertEquals("", accessor.get("添付ファイル（null要素）"));
        assertEquals("", accessor.get("添付ファイル（null項目）"));
        assertEquals("[]", accessor.get("テーブル（空）"));
        assertEquals("[null,null]", accessor.get("テーブル（null要素）"));
        assertEquals("[{\"value\":{\"添付ファイル（null要素）\":{\"type\":\"FILE\",\"value\":[null,null]},\"添付ファイル（null項目）\":{\"type\":\"FILE\",\"value\":[{},{}]},\"複数選択（空）\":{\"type\":\"MULTI_SELECT\",\"value\":[]},\"リッチエディター\":{\"type\":\"RICH_TEXT\"},\"文字列（1行）\":{\"type\":\"SINGLE_LINE_TEXT\"},\"ユーザー選択（null項目）\":{\"type\":\"USER_SELECT\",\"value\":[{},{}]},\"文字列（複数行）\":{\"type\":\"MULTI_LINE_TEXT\"},\"ユーザー選択（空）\":{\"type\":\"USER_SELECT\",\"value\":[]},\"チェックボックス（null要素）\":{\"type\":\"CHECK_BOX\",\"value\":[null,null]},\"組織選択（null項目）\":{\"type\":\"ORGANIZATION_SELECT\",\"value\":[{},{}]},\"計算\":{\"type\":\"CALC\"},\"日付\":{\"type\":\"DATE\"},\"組織選択（空）\":{\"type\":\"ORGANIZATION_SELECT\",\"value\":[]},\"添付ファイル（空）\":{\"type\":\"FILE\",\"value\":[]},\"ラジオボタン\":{\"type\":\"RADIO_BUTTON\"},\"グループ選択（null項目）\":{\"type\":\"GROUP_SELECT\",\"value\":[{},{}]},\"複数選択（null要素）\":{\"type\":\"MULTI_SELECT\",\"value\":[null,null]},\"ドロップダウン\":{\"type\":\"DROP_DOWN\"},\"日時\":{\"type\":\"DATETIME\"},\"組織選択（null要素）\":{\"type\":\"ORGANIZATION_SELECT\",\"value\":[null,null]},\"時刻\":{\"type\":\"TIME\"},\"グループ選択（空）\":{\"type\":\"GROUP_SELECT\",\"value\":[]},\"数値\":{\"type\":\"NUMBER\"},\"ユーザー選択（null要素）\":{\"type\":\"USER_SELECT\",\"value\":[null,null]},\"グループ選択（null要素）\":{\"type\":\"GROUP_SELECT\",\"value\":[null,null]},\"リンク\":{\"type\":\"LINK\"},\"チェックボックス（空）\":{\"type\":\"CHECK_BOX\",\"value\":[]}}},{\"value\":{\"添付ファイル（null要素）\":{\"type\":\"FILE\",\"value\":[null,null]},\"添付ファイル（null項目）\":{\"type\":\"FILE\",\"value\":[{},{}]},\"複数選択（空）\":{\"type\":\"MULTI_SELECT\",\"value\":[]},\"リッチエディター\":{\"type\":\"RICH_TEXT\"},\"文字列（1行）\":{\"type\":\"SINGLE_LINE_TEXT\"},\"ユーザー選択（null項目）\":{\"type\":\"USER_SELECT\",\"value\":[{},{}]},\"文字列（複数行）\":{\"type\":\"MULTI_LINE_TEXT\"},\"ユーザー選択（空）\":{\"type\":\"USER_SELECT\",\"value\":[]},\"チェックボックス（null要素）\":{\"type\":\"CHECK_BOX\",\"value\":[null,null]},\"組織選択（null項目）\":{\"type\":\"ORGANIZATION_SELECT\",\"value\":[{},{}]},\"計算\":{\"type\":\"CALC\"},\"日付\":{\"type\":\"DATE\"},\"組織選択（空）\":{\"type\":\"ORGANIZATION_SELECT\",\"value\":[]},\"添付ファイル（空）\":{\"type\":\"FILE\",\"value\":[]},\"ラジオボタン\":{\"type\":\"RADIO_BUTTON\"},\"グループ選択（null項目）\":{\"type\":\"GROUP_SELECT\",\"value\":[{},{}]},\"複数選択（null要素）\":{\"type\":\"MULTI_SELECT\",\"value\":[null,null]},\"ドロップダウン\":{\"type\":\"DROP_DOWN\"},\"日時\":{\"type\":\"DATETIME\"},\"組織選択（null要素）\":{\"type\":\"ORGANIZATION_SELECT\",\"value\":[null,null]},\"時刻\":{\"type\":\"TIME\"},\"グループ選択（空）\":{\"type\":\"GROUP_SELECT\",\"value\":[]},\"数値\":{\"type\":\"NUMBER\"},\"ユーザー選択（null要素）\":{\"type\":\"USER_SELECT\",\"value\":[null,null]},\"グループ選択（null要素）\":{\"type\":\"GROUP_SELECT\",\"value\":[null,null]},\"リンク\":{\"type\":\"LINK\"},\"チェックボックス（空）\":{\"type\":\"CHECK_BOX\",\"value\":[]}}}]", accessor.get("テーブル（null項目）"));
        assertEquals("", accessor.get("カテゴリー（空）"));
        /* ビルトインフィールドは 1 つしか追加できない
        assertEquals("", accessor.get("カテゴリー（null要素）"));
         */
        assertNull(accessor.get("ステータス"));
        assertEquals("", accessor.get("作業者（空）"));
        /* ビルトインフィールドは 1 つしか追加できない
        assertEquals("", accessor.get("作業者（null要素）"));
        assertEquals("", accessor.get("作業者（null項目）"));
         */
    }

    private Record nullRecord()
    {
        final Record record = new Record();
        record.putField("レコード番号", new RecordNumberFieldValue(null));
        record.putField("作成者", new CreatorFieldValue(null));
        record.putField("作成者（null項目）", new CreatorFieldValue(user(null, null)));
        record.putField("作成日時", new CreatedTimeFieldValue(null));
        record.putField("更新者", new ModifierFieldValue(null));
        record.putField("更新者（null項目）", new ModifierFieldValue(user(null, null)));
        record.putField("更新日時", new UpdatedTimeFieldValue(null));
        record.putField("文字列（1行）", new SingleLineTextFieldValue(null));
        record.putField("文字列（複数行）", new MultiLineTextFieldValue(null));
        record.putField("リッチエディター", new RichTextFieldValue(null));
        record.putField("数値", new NumberFieldValue(null));
        record.putField("計算", new CalcFieldValue(null));
        record.putField("チェックボックス（空）", new CheckBoxFieldValue());
        record.putField("チェックボックス（null要素）", new CheckBoxFieldValue(null, null));
        record.putField("ラジオボタン", new RadioButtonFieldValue(null));
        record.putField("複数選択（空）", new MultiSelectFieldValue());
        record.putField("複数選択（null要素）", new MultiSelectFieldValue(null, null));
        record.putField("ドロップダウン", new DropDownFieldValue(null));
        record.putField("ユーザー選択（空）", new UserSelectFieldValue());
        record.putField("ユーザー選択（null要素）", new UserSelectFieldValue(null, null));
        record.putField("ユーザー選択（null項目）", new UserSelectFieldValue(user(null, null), user(null, null)));
        record.putField("組織選択（空）", new OrganizationSelectFieldValue());
        record.putField("組織選択（null要素）", new OrganizationSelectFieldValue(null, null));
        record.putField("組織選択（null項目）", new OrganizationSelectFieldValue(organization(null, null), organization(null, null)));
        record.putField("グループ選択（空）", new GroupSelectFieldValue());
        record.putField("グループ選択（null要素）", new GroupSelectFieldValue(null, null));
        record.putField("グループ選択（null項目）", new GroupSelectFieldValue(group(null, null), group(null, null)));
        record.putField("日付", new DateFieldValue(null));
        record.putField("時刻", new TimeFieldValue(null));
        record.putField("日時", new DateTimeFieldValue(null));
        record.putField("リンク", new LinkFieldValue(null));
        record.putField("添付ファイル（空）", new FileFieldValue());
        record.putField("添付ファイル（null要素）", new FileFieldValue(null, null));
        record.putField("添付ファイル（null項目）", new FileFieldValue(file(null, null, null, null), file(null, null, null, null)));
        record.putField("テーブル（空）", new SubtableFieldValue());
        record.putField("テーブル（null要素）", new SubtableFieldValue(null, null));
        record.putField("テーブル（null項目）", new SubtableFieldValue(nullTableRow(), nullTableRow()));
        record.putField("カテゴリー（空）", new CategoryFieldValue());
        /* ビルトインフィールドは 1 つしか追加できない
        record.putField("カテゴリー（null要素）", new CategoryFieldValue(null, null));
         */
        record.putField("ステータス", new StatusFieldValue(null));
        record.putField("作業者（空）", new StatusAssigneeFieldValue());
        /* ビルトインフィールドは 1 つしか追加できない
        record.putField("作業者（null要素）", new StatusAssigneeFieldValue(null, null));
        record.putField("作業者（null項目）", new StatusAssigneeFieldValue(user(null, null), user(null, null)));
         */
        return record;
    }

    private TableRow nullTableRow()
    {
        final TableRow tableRow = new TableRow();
        tableRow.putField("文字列（1行）", new SingleLineTextFieldValue(null));
        tableRow.putField("文字列（複数行）", new MultiLineTextFieldValue(null));
        tableRow.putField("リッチエディター", new RichTextFieldValue(null));
        tableRow.putField("数値", new NumberFieldValue(null));
        tableRow.putField("計算", new CalcFieldValue(null));
        tableRow.putField("チェックボックス（空）", new CheckBoxFieldValue());
        tableRow.putField("チェックボックス（null要素）", new CheckBoxFieldValue(null, null));
        tableRow.putField("ラジオボタン", new RadioButtonFieldValue(null));
        tableRow.putField("複数選択（空）", new MultiSelectFieldValue());
        tableRow.putField("複数選択（null要素）", new MultiSelectFieldValue(null, null));
        tableRow.putField("ドロップダウン", new DropDownFieldValue(null));
        tableRow.putField("ユーザー選択（空）", new UserSelectFieldValue());
        tableRow.putField("ユーザー選択（null要素）", new UserSelectFieldValue(null, null));
        tableRow.putField("ユーザー選択（null項目）", new UserSelectFieldValue(user(null, null), user(null, null)));
        tableRow.putField("組織選択（空）", new OrganizationSelectFieldValue());
        tableRow.putField("組織選択（null要素）", new OrganizationSelectFieldValue(null, null));
        tableRow.putField("組織選択（null項目）", new OrganizationSelectFieldValue(organization(null, null), organization(null, null)));
        tableRow.putField("グループ選択（空）", new GroupSelectFieldValue());
        tableRow.putField("グループ選択（null要素）", new GroupSelectFieldValue(null, null));
        tableRow.putField("グループ選択（null項目）", new GroupSelectFieldValue(group(null, null), group(null, null)));
        tableRow.putField("日付", new DateFieldValue(null));
        tableRow.putField("時刻", new TimeFieldValue(null));
        tableRow.putField("日時", new DateTimeFieldValue(null));
        tableRow.putField("リンク", new LinkFieldValue(null));
        tableRow.putField("添付ファイル（空）", new FileFieldValue());
        tableRow.putField("添付ファイル（null要素）", new FileFieldValue(null, null));
        tableRow.putField("添付ファイル（null項目）", new FileFieldValue(file(null, null, null, null), file(null, null, null, null)));
        return tableRow;
    }

    private User user(final String code, final String name)
    {
        return new User(name, code);
    }

    private Organization organization(final String code, final String name)
    {
        return new Organization(name, code);
    }

    private Group group(final String code, final String name)
    {
        return new Group(name, code);
    }

    private FileBody file(final String contentType, final String fileKey, final String name, final String size)
    {
        final FileBody file = new FileBody();
        file.setContentType(contentType);
        file.setFileKey(fileKey);
        file.setName(name);
        file.setSize(size == null ? null : Integer.valueOf(size));
        return file;
    }
}
