package org.embulk.input.kintone;


import com.cybozu.kintone.client.model.app.form.FieldType;
import com.cybozu.kintone.client.model.file.FileModel;
import com.cybozu.kintone.client.model.record.field.FieldValue;
import com.cybozu.kintone.client.model.record.SubTableValueItem;
import com.cybozu.kintone.client.model.member.Member;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
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
        assertEquals("[{\"id\":48290,\"value\":{\"リッチエディター\":{\"type\":\"RICH_TEXT\",\"value\":\"\\u003ca href\\u003d\\\"https://www.cybozu.com\\\"\\u003eサイボウズ\\u003c/a\\u003e\"},\"グループ選択\":{\"type\":\"GROUP_SELECT\",\"value\":[{\"code\":\"project_manager\",\"name\":\"プロジェクトマネージャー\"},{\"code\":\"team_leader\",\"name\":\"チームリーダー\"}]},\"文字列（1行）\":{\"type\":\"SINGLE_LINE_TEXT\",\"value\":\"テストです。\"},\"ラジオボタン\":{\"type\":\"RADIO_BUTTON\",\"value\":\"選択肢3\"},\"ドロップダウン\":{\"type\":\"DROP_DOWN\",\"value\":\"選択肢6\"},\"組織選択\":{\"type\":\"ORGANIZATION_SELECT\",\"value\":[{\"code\":\"kaihatsu\",\"name\":\"開発部\"},{\"code\":\"jinji\",\"name\":\"人事部\"}]},\"ユーザー選択\":{\"type\":\"USER_SELECT\",\"value\":[{\"code\":\"guest/sato@cybozu.com\",\"name\":\"Noboru Sato\"},{\"code\":\"kato\",\"name\":\"Misaki Kato\"}]},\"日時\":{\"type\":\"DATETIME\",\"value\":\"2012-01-11T11:30:00Z\"},\"文字列（複数行）\":{\"type\":\"MULTI_LINE_TEXT\",\"value\":\"テスト\\nです。\"},\"時刻\":{\"type\":\"TIME\",\"value\":\"11:30\"},\"チェックボックス\":{\"type\":\"CHECK_BOX\",\"value\":[\"選択肢1\",\"選択肢2\"]},\"複数選択\":{\"type\":\"MULTI_SELECT\",\"value\":[\"選択肢4\",\"選択肢5\"]},\"数値\":{\"type\":\"NUMBER\",\"value\":\"123\"},\"添付ファイル\":{\"type\":\"FILE\",\"value\":[{\"contentType\":\"text/plain\",\"fileKey\":\"201202061155587E339F9067544F1A92C743460E3D12B3297\",\"name\":\"17to20_VerupLog (1).txt\",\"size\":\"23175\"},{\"contentType\":\"application/json\",\"fileKey\":\"201202061155583C763E30196F419E83E91D2E4A03746C273\",\"name\":\"17to20_VerupLog.txt\",\"size\":\"23176\"}]},\"リンク\":{\"type\":\"LINK\",\"value\":\"https://cybozu.co.jp/\"},\"計算\":{\"type\":\"CALC\",\"value\":\"456\"},\"日付\":{\"type\":\"DATE\",\"value\":\"2012-01-11\"}}},{\"id\":48291,\"value\":{\"リッチエディター\":{\"type\":\"RICH_TEXT\",\"value\":\"\\u003ca href\\u003d\\\"https://www.cybozu.com\\\"\\u003eサイボウズ\\u003c/a\\u003e\"},\"グループ選択\":{\"type\":\"GROUP_SELECT\",\"value\":[{\"code\":\"project_manager\",\"name\":\"プロジェクトマネージャー\"},{\"code\":\"team_leader\",\"name\":\"チームリーダー\"}]},\"文字列（1行）\":{\"type\":\"SINGLE_LINE_TEXT\",\"value\":\"テストです。\"},\"ラジオボタン\":{\"type\":\"RADIO_BUTTON\",\"value\":\"選択肢3\"},\"ドロップダウン\":{\"type\":\"DROP_DOWN\",\"value\":\"選択肢6\"},\"組織選択\":{\"type\":\"ORGANIZATION_SELECT\",\"value\":[{\"code\":\"kaihatsu\",\"name\":\"開発部\"},{\"code\":\"jinji\",\"name\":\"人事部\"}]},\"ユーザー選択\":{\"type\":\"USER_SELECT\",\"value\":[{\"code\":\"guest/sato@cybozu.com\",\"name\":\"Noboru Sato\"},{\"code\":\"kato\",\"name\":\"Misaki Kato\"}]},\"日時\":{\"type\":\"DATETIME\",\"value\":\"2012-01-11T11:30:00Z\"},\"文字列（複数行）\":{\"type\":\"MULTI_LINE_TEXT\",\"value\":\"テスト\\nです。\"},\"時刻\":{\"type\":\"TIME\",\"value\":\"11:30\"},\"チェックボックス\":{\"type\":\"CHECK_BOX\",\"value\":[\"選択肢1\",\"選択肢2\"]},\"複数選択\":{\"type\":\"MULTI_SELECT\",\"value\":[\"選択肢4\",\"選択肢5\"]},\"数値\":{\"type\":\"NUMBER\",\"value\":\"123\"},\"添付ファイル\":{\"type\":\"FILE\",\"value\":[{\"contentType\":\"text/plain\",\"fileKey\":\"201202061155587E339F9067544F1A92C743460E3D12B3297\",\"name\":\"17to20_VerupLog (1).txt\",\"size\":\"23175\"},{\"contentType\":\"application/json\",\"fileKey\":\"201202061155583C763E30196F419E83E91D2E4A03746C273\",\"name\":\"17to20_VerupLog.txt\",\"size\":\"23176\"}]},\"リンク\":{\"type\":\"LINK\",\"value\":\"https://cybozu.co.jp/\"},\"計算\":{\"type\":\"CALC\",\"value\":\"456\"},\"日付\":{\"type\":\"DATE\",\"value\":\"2012-01-11\"}}}]", accessor.get("テーブル"));
        assertEquals("category1\ncategory2", accessor.get("カテゴリー"));
        assertEquals("未処理", accessor.get("ステータス"));
        assertEquals("sato\nkato", accessor.get("作業者"));
        assertEquals("", accessor.get("関連レコード一覧"));
        assertEquals("", accessor.get("ラベル"));
        assertEquals("", accessor.get("スペース"));
        assertEquals("", accessor.get("罫線"));
        assertEquals("", accessor.get("グループ"));
    }

    private HashMap<String, FieldValue> record()
    {
        final HashMap<String, FieldValue> record = new HashMap<>();
        record.put("レコード番号", field(FieldType.RECORD_NUMBER, "APPCODE-1"));
        record.put("$id", field(FieldType.__ID__, "1"));
        record.put("$revision", field(FieldType.__REVISION__, "5"));
        record.put("作成者", field(FieldType.CREATOR, member("sato", "Noboru Sato")));
        record.put("作成日時", field(FieldType.CREATED_TIME, "2021-01-11T11:11:11Z"));
        record.put("更新者", field(FieldType.MODIFIER, member("guest/kato@cybozu.com", "Misaki Kato")));
        record.put("更新日時", field(FieldType.UPDATED_TIME, "2022-02-22T22:22:22Z"));
        record.put("文字列（1行）", field(FieldType.SINGLE_LINE_TEXT, "テストです。"));
        record.put("文字列（複数行）", field(FieldType.MULTI_LINE_TEXT, "テスト\nです。"));
        record.put("リッチエディター", field(FieldType.RICH_TEXT, "<a href=\"https://www.cybozu.com\">サイボウズ</a>"));
        record.put("数値", field(FieldType.NUMBER, "123"));
        record.put("計算", field(FieldType.CALC, "456"));
        record.put("チェックボックス", field(FieldType.CHECK_BOX, arrayList("選択肢1", "選択肢2")));
        record.put("ラジオボタン", field(FieldType.RADIO_BUTTON, "選択肢3"));
        record.put("複数選択", field(FieldType.MULTI_SELECT, arrayList("選択肢4", "選択肢5")));
        record.put("ドロップダウン", field(FieldType.DROP_DOWN, "選択肢6"));
        record.put("ユーザー選択", field(FieldType.USER_SELECT, arrayList(member("guest/sato@cybozu.com", "Noboru Sato"), member("kato", "Misaki Kato"))));
        record.put("組織選択", field(FieldType.ORGANIZATION_SELECT, arrayList(member("kaihatsu", "開発部"), member("jinji", "人事部"))));
        record.put("グループ選択", field(FieldType.GROUP_SELECT, arrayList(member("project_manager", "プロジェクトマネージャー"), member("team_leader", "チームリーダー"))));
        record.put("日付", field(FieldType.DATE, "2012-01-11"));
        record.put("時刻", field(FieldType.TIME, "11:30"));
        record.put("日時", field(FieldType.DATETIME, "2012-01-11T11:30:00Z"));
        record.put("リンク", field(FieldType.LINK, "https://cybozu.co.jp/"));
        record.put("添付ファイル", field(FieldType.FILE, arrayList(file("text/plain", "201202061155587E339F9067544F1A92C743460E3D12B3297", "17to20_VerupLog (1).txt", "23175"), file("application/json", "201202061155583C763E30196F419E83E91D2E4A03746C273", "17to20_VerupLog.txt", "23176"))));
        record.put("テーブル", field(FieldType.SUBTABLE, arrayList(subTableValueItem(48290), subTableValueItem(48291))));
        record.put("カテゴリー", field(FieldType.CATEGORY, arrayList("category1", "category2")));
        record.put("ステータス", field(FieldType.STATUS, "未処理"));
        record.put("作業者", field(FieldType.STATUS_ASSIGNEE, arrayList(member("sato", "Noboru Sato"), member("kato", "Misaki Kato"))));
        record.put("関連レコード一覧", field(FieldType.REFERENCE_TABLE, ""));
        record.put("ラベル", field(FieldType.LABEL, ""));
        record.put("スペース", field(FieldType.SPACER, ""));
        record.put("罫線", field(FieldType.HR, ""));
        record.put("グループ", field(FieldType.GROUP, ""));
        return record;
    }

    private SubTableValueItem subTableValueItem(final Integer id)
    {
        final SubTableValueItem subTableValueItem = new SubTableValueItem();
        subTableValueItem.setID(id);
        final HashMap<String, FieldValue> value = new HashMap<>();
        value.put("文字列（1行）", field(FieldType.SINGLE_LINE_TEXT, "テストです。"));
        value.put("文字列（複数行）", field(FieldType.MULTI_LINE_TEXT, "テスト\nです。"));
        value.put("リッチエディター", field(FieldType.RICH_TEXT, "<a href=\"https://www.cybozu.com\">サイボウズ</a>"));
        value.put("数値", field(FieldType.NUMBER, "123"));
        value.put("計算", field(FieldType.CALC, "456"));
        value.put("チェックボックス", field(FieldType.CHECK_BOX, arrayList("選択肢1", "選択肢2")));
        value.put("ラジオボタン", field(FieldType.RADIO_BUTTON, "選択肢3"));
        value.put("複数選択", field(FieldType.MULTI_SELECT, arrayList("選択肢4", "選択肢5")));
        value.put("ドロップダウン", field(FieldType.DROP_DOWN, "選択肢6"));
        value.put("ユーザー選択", field(FieldType.USER_SELECT, arrayList(member("guest/sato@cybozu.com", "Noboru Sato"), member("kato", "Misaki Kato"))));
        value.put("組織選択", field(FieldType.ORGANIZATION_SELECT, arrayList(member("kaihatsu", "開発部"), member("jinji", "人事部"))));
        value.put("グループ選択", field(FieldType.GROUP_SELECT, arrayList(member("project_manager", "プロジェクトマネージャー"), member("team_leader", "チームリーダー"))));
        value.put("日付", field(FieldType.DATE, "2012-01-11"));
        value.put("時刻", field(FieldType.TIME, "11:30"));
        value.put("日時", field(FieldType.DATETIME, "2012-01-11T11:30:00Z"));
        value.put("リンク", field(FieldType.LINK, "https://cybozu.co.jp/"));
        value.put("添付ファイル", field(FieldType.FILE, arrayList(file("text/plain", "201202061155587E339F9067544F1A92C743460E3D12B3297", "17to20_VerupLog (1).txt", "23175"), file("application/json", "201202061155583C763E30196F419E83E91D2E4A03746C273", "17to20_VerupLog.txt", "23176"))));
        subTableValueItem.setValue(value);
        return subTableValueItem;
    }

    @Test
    public void testNullFields()
    {
        final KintoneAccessor accessor = new KintoneAccessor(nullRecord());
        assertNull(accessor.get("レコード番号"));
        assertNull(accessor.get("$id"));
        assertNull(accessor.get("$revision"));
        assertThrows(NullPointerException.class, () -> accessor.get("作成者"));
        assertNull(accessor.get("作成者（null項目）"));
        assertNull(accessor.get("作成日時"));
        assertThrows(NullPointerException.class, () -> accessor.get("更新者"));
        assertNull(accessor.get("更新者（null項目）"));
        assertNull(accessor.get("更新日時"));
        assertNull(accessor.get("文字列（1行）"));
        assertNull(accessor.get("文字列（複数行）"));
        assertNull(accessor.get("リッチエディター"));
        assertEquals("null", accessor.get("数値"));
        assertNull(accessor.get("計算"));
        assertThrows(NullPointerException.class, () -> accessor.get("チェックボックス"));
        assertEquals("", accessor.get("チェックボックス（空）"));
        assertEquals("null\nnull", accessor.get("チェックボックス（null要素）"));
        assertNull(accessor.get("ラジオボタン"));
        assertThrows(NullPointerException.class, () -> accessor.get("複数選択"));
        assertEquals("", accessor.get("複数選択（空）"));
        assertEquals("null\nnull", accessor.get("複数選択（null要素）"));
        assertNull(accessor.get("ドロップダウン"));
        assertThrows(NullPointerException.class, () -> accessor.get("ユーザー選択"));
        assertEquals("", accessor.get("ユーザー選択（空）"));
        assertThrows(NullPointerException.class, () -> accessor.get("ユーザー選択（null要素）"));
        assertEquals("null\nnull", accessor.get("ユーザー選択（null項目）"));
        assertThrows(NullPointerException.class, () -> accessor.get("組織選択"));
        assertEquals("", accessor.get("組織選択（空）"));
        assertThrows(NullPointerException.class, () -> accessor.get("組織選択（null要素）"));
        assertEquals("null\nnull", accessor.get("組織選択（null項目）"));
        assertThrows(NullPointerException.class, () -> accessor.get("グループ選択"));
        assertEquals("", accessor.get("グループ選択（空）"));
        assertThrows(NullPointerException.class, () -> accessor.get("グループ選択（null要素）"));
        assertEquals("null\nnull", accessor.get("グループ選択（null項目）"));
        assertNull(accessor.get("日付"));
        assertNull(accessor.get("時刻"));
        assertNull(accessor.get("日時"));
        assertNull(accessor.get("リンク"));
        assertThrows(NullPointerException.class, () -> accessor.get("添付ファイル"));
        assertEquals("", accessor.get("添付ファイル（空）"));
        assertThrows(NullPointerException.class, () -> accessor.get("添付ファイル（null要素）"));
        assertEquals("null\nnull", accessor.get("添付ファイル（null項目）"));
        assertEquals("null", accessor.get("テーブル"));
        assertEquals("[]", accessor.get("テーブル（空）"));
        assertEquals("[null,null]", accessor.get("テーブル（null要素）"));
        assertEquals("[{\"value\":{\"添付ファイル（null要素）\":{\"type\":\"FILE\",\"value\":[null,null]},\"添付ファイル（null項目）\":{\"type\":\"FILE\",\"value\":[{},{}]},\"複数選択（空）\":{\"type\":\"MULTI_SELECT\",\"value\":[]},\"リッチエディター\":{\"type\":\"RICH_TEXT\"},\"文字列（1行）\":{\"type\":\"SINGLE_LINE_TEXT\"},\"ユーザー選択（null項目）\":{\"type\":\"USER_SELECT\",\"value\":[{},{}]},\"ユーザー選択\":{\"type\":\"USER_SELECT\"},\"文字列（複数行）\":{\"type\":\"MULTI_LINE_TEXT\"},\"ユーザー選択（空）\":{\"type\":\"USER_SELECT\",\"value\":[]},\"チェックボックス（null要素）\":{\"type\":\"CHECK_BOX\",\"value\":[null,null]},\"組織選択（null項目）\":{\"type\":\"ORGANIZATION_SELECT\",\"value\":[{},{}]},\"計算\":{\"type\":\"CALC\"},\"日付\":{\"type\":\"DATE\"},\"組織選択（空）\":{\"type\":\"ORGANIZATION_SELECT\",\"value\":[]},\"添付ファイル（空）\":{\"type\":\"FILE\",\"value\":[]},\"グループ選択\":{\"type\":\"GROUP_SELECT\"},\"ラジオボタン\":{\"type\":\"RADIO_BUTTON\"},\"グループ選択（null項目）\":{\"type\":\"GROUP_SELECT\",\"value\":[{},{}]},\"複数選択（null要素）\":{\"type\":\"MULTI_SELECT\",\"value\":[null,null]},\"ドロップダウン\":{\"type\":\"DROP_DOWN\"},\"組織選択\":{\"type\":\"ORGANIZATION_SELECT\"},\"日時\":{\"type\":\"DATETIME\"},\"組織選択（null要素）\":{\"type\":\"ORGANIZATION_SELECT\",\"value\":[null,null]},\"時刻\":{\"type\":\"TIME\"},\"チェックボックス\":{\"type\":\"CHECK_BOX\"},\"グループ選択（空）\":{\"type\":\"GROUP_SELECT\",\"value\":[]},\"複数選択\":{\"type\":\"MULTI_SELECT\"},\"数値\":{\"type\":\"NUMBER\"},\"ユーザー選択（null要素）\":{\"type\":\"USER_SELECT\",\"value\":[null,null]},\"グループ選択（null要素）\":{\"type\":\"GROUP_SELECT\",\"value\":[null,null]},\"添付ファイル\":{\"type\":\"FILE\"},\"リンク\":{\"type\":\"LINK\"},\"チェックボックス（空）\":{\"type\":\"CHECK_BOX\",\"value\":[]}}},{\"value\":{\"添付ファイル（null要素）\":{\"type\":\"FILE\",\"value\":[null,null]},\"添付ファイル（null項目）\":{\"type\":\"FILE\",\"value\":[{},{}]},\"複数選択（空）\":{\"type\":\"MULTI_SELECT\",\"value\":[]},\"リッチエディター\":{\"type\":\"RICH_TEXT\"},\"文字列（1行）\":{\"type\":\"SINGLE_LINE_TEXT\"},\"ユーザー選択（null項目）\":{\"type\":\"USER_SELECT\",\"value\":[{},{}]},\"ユーザー選択\":{\"type\":\"USER_SELECT\"},\"文字列（複数行）\":{\"type\":\"MULTI_LINE_TEXT\"},\"ユーザー選択（空）\":{\"type\":\"USER_SELECT\",\"value\":[]},\"チェックボックス（null要素）\":{\"type\":\"CHECK_BOX\",\"value\":[null,null]},\"組織選択（null項目）\":{\"type\":\"ORGANIZATION_SELECT\",\"value\":[{},{}]},\"計算\":{\"type\":\"CALC\"},\"日付\":{\"type\":\"DATE\"},\"組織選択（空）\":{\"type\":\"ORGANIZATION_SELECT\",\"value\":[]},\"添付ファイル（空）\":{\"type\":\"FILE\",\"value\":[]},\"グループ選択\":{\"type\":\"GROUP_SELECT\"},\"ラジオボタン\":{\"type\":\"RADIO_BUTTON\"},\"グループ選択（null項目）\":{\"type\":\"GROUP_SELECT\",\"value\":[{},{}]},\"複数選択（null要素）\":{\"type\":\"MULTI_SELECT\",\"value\":[null,null]},\"ドロップダウン\":{\"type\":\"DROP_DOWN\"},\"組織選択\":{\"type\":\"ORGANIZATION_SELECT\"},\"日時\":{\"type\":\"DATETIME\"},\"組織選択（null要素）\":{\"type\":\"ORGANIZATION_SELECT\",\"value\":[null,null]},\"時刻\":{\"type\":\"TIME\"},\"チェックボックス\":{\"type\":\"CHECK_BOX\"},\"グループ選択（空）\":{\"type\":\"GROUP_SELECT\",\"value\":[]},\"複数選択\":{\"type\":\"MULTI_SELECT\"},\"数値\":{\"type\":\"NUMBER\"},\"ユーザー選択（null要素）\":{\"type\":\"USER_SELECT\",\"value\":[null,null]},\"グループ選択（null要素）\":{\"type\":\"GROUP_SELECT\",\"value\":[null,null]},\"添付ファイル\":{\"type\":\"FILE\"},\"リンク\":{\"type\":\"LINK\"},\"チェックボックス（空）\":{\"type\":\"CHECK_BOX\",\"value\":[]}}}]", accessor.get("テーブル（null項目）"));
        assertThrows(NullPointerException.class, () -> accessor.get("カテゴリー"));
        assertEquals("", accessor.get("カテゴリー（空）"));
        assertEquals("null\nnull", accessor.get("カテゴリー（null要素）"));
        assertNull(accessor.get("ステータス"));
        assertThrows(NullPointerException.class, () -> accessor.get("作業者"));
        assertEquals("", accessor.get("作業者（空）"));
        assertThrows(NullPointerException.class, () -> accessor.get("作業者（null要素）"));
        assertEquals("null\nnull", accessor.get("作業者（null項目）"));
        assertNull(accessor.get("関連レコード一覧"));
        assertNull(accessor.get("ラベル"));
        assertNull(accessor.get("スペース"));
        assertNull(accessor.get("罫線"));
        assertNull(accessor.get("グループ"));
    }

    private HashMap<String, FieldValue> nullRecord()
    {
        final HashMap<String, FieldValue> record = new HashMap<>();
        record.put("レコード番号", field(FieldType.RECORD_NUMBER, null));
        record.put("$id", field(FieldType.__ID__, null));
        record.put("$revision", field(FieldType.__REVISION__, null));
        record.put("作成者", field(FieldType.CREATOR, null));
        record.put("作成者（null項目）", field(FieldType.CREATOR, member(null, null)));
        record.put("作成日時", field(FieldType.CREATED_TIME, null));
        record.put("更新者", field(FieldType.MODIFIER, null));
        record.put("更新者（null項目）", field(FieldType.MODIFIER, member(null, null)));
        record.put("更新日時", field(FieldType.UPDATED_TIME, null));
        record.put("文字列（1行）", field(FieldType.SINGLE_LINE_TEXT, null));
        record.put("文字列（複数行）", field(FieldType.MULTI_LINE_TEXT, null));
        record.put("リッチエディター", field(FieldType.RICH_TEXT, null));
        record.put("数値", field(FieldType.NUMBER, null));
        record.put("計算", field(FieldType.CALC, null));
        record.put("チェックボックス", field(FieldType.CHECK_BOX, null));
        record.put("チェックボックス（空）", field(FieldType.CHECK_BOX, arrayList()));
        record.put("チェックボックス（null要素）", field(FieldType.CHECK_BOX, arrayList(null, null)));
        record.put("ラジオボタン", field(FieldType.RADIO_BUTTON, null));
        record.put("複数選択", field(FieldType.MULTI_SELECT, null));
        record.put("複数選択（空）", field(FieldType.MULTI_SELECT, arrayList()));
        record.put("複数選択（null要素）", field(FieldType.MULTI_SELECT, arrayList(null, null)));
        record.put("ドロップダウン", field(FieldType.DROP_DOWN, null));
        record.put("ユーザー選択", field(FieldType.USER_SELECT, null));
        record.put("ユーザー選択（空）", field(FieldType.USER_SELECT, arrayList()));
        record.put("ユーザー選択（null要素）", field(FieldType.USER_SELECT, arrayList(null, null)));
        record.put("ユーザー選択（null項目）", field(FieldType.USER_SELECT, arrayList(member(null, null), member(null, null))));
        record.put("組織選択", field(FieldType.ORGANIZATION_SELECT, null));
        record.put("組織選択（空）", field(FieldType.ORGANIZATION_SELECT, arrayList()));
        record.put("組織選択（null要素）", field(FieldType.ORGANIZATION_SELECT, arrayList(null, null)));
        record.put("組織選択（null項目）", field(FieldType.ORGANIZATION_SELECT, arrayList(member(null, null), member(null, null))));
        record.put("グループ選択", field(FieldType.GROUP_SELECT, null));
        record.put("グループ選択（空）", field(FieldType.GROUP_SELECT, arrayList()));
        record.put("グループ選択（null要素）", field(FieldType.GROUP_SELECT, arrayList(null, null)));
        record.put("グループ選択（null項目）", field(FieldType.GROUP_SELECT, arrayList(member(null, null), member(null, null))));
        record.put("日付", field(FieldType.DATE, null));
        record.put("時刻", field(FieldType.TIME, null));
        record.put("日時", field(FieldType.DATETIME, null));
        record.put("リンク", field(FieldType.LINK, null));
        record.put("添付ファイル", field(FieldType.FILE, null));
        record.put("添付ファイル（空）", field(FieldType.FILE, arrayList()));
        record.put("添付ファイル（null要素）", field(FieldType.FILE, arrayList(null, null)));
        record.put("添付ファイル（null項目）", field(FieldType.FILE, arrayList(file(null, null, null, null), file(null, null, null, null))));
        record.put("テーブル", field(FieldType.SUBTABLE, null));
        record.put("テーブル（空）", field(FieldType.SUBTABLE, arrayList()));
        record.put("テーブル（null要素）", field(FieldType.SUBTABLE, arrayList(null, null)));
        record.put("テーブル（null項目）", field(FieldType.SUBTABLE, arrayList(nullSubTableValueItem(), nullSubTableValueItem())));
        record.put("カテゴリー", field(FieldType.CATEGORY, null));
        record.put("カテゴリー（空）", field(FieldType.CATEGORY, arrayList()));
        record.put("カテゴリー（null要素）", field(FieldType.CATEGORY, arrayList(null, null)));
        record.put("ステータス", field(FieldType.STATUS, null));
        record.put("作業者", field(FieldType.STATUS_ASSIGNEE, null));
        record.put("作業者（空）", field(FieldType.STATUS_ASSIGNEE, arrayList()));
        record.put("作業者（null要素）", field(FieldType.STATUS_ASSIGNEE, arrayList(null, null)));
        record.put("作業者（null項目）", field(FieldType.STATUS_ASSIGNEE, arrayList(member(null, null), member(null, null))));
        record.put("関連レコード一覧", field(FieldType.REFERENCE_TABLE, null));
        record.put("ラベル", field(FieldType.LABEL, null));
        record.put("スペース", field(FieldType.SPACER, null));
        record.put("罫線", field(FieldType.HR, null));
        record.put("グループ", field(FieldType.GROUP, null));
        return record;
    }

    private SubTableValueItem nullSubTableValueItem()
    {
        final SubTableValueItem subTableValueItem = new SubTableValueItem();
        subTableValueItem.setID(null);
        final HashMap<String, FieldValue> value = new HashMap<>();
        value.put("文字列（1行）", field(FieldType.SINGLE_LINE_TEXT, null));
        value.put("文字列（複数行）", field(FieldType.MULTI_LINE_TEXT, null));
        value.put("リッチエディター", field(FieldType.RICH_TEXT, null));
        value.put("数値", field(FieldType.NUMBER, null));
        value.put("計算", field(FieldType.CALC, null));
        value.put("チェックボックス", field(FieldType.CHECK_BOX, null));
        value.put("チェックボックス（空）", field(FieldType.CHECK_BOX, arrayList()));
        value.put("チェックボックス（null要素）", field(FieldType.CHECK_BOX, arrayList(null, null)));
        value.put("ラジオボタン", field(FieldType.RADIO_BUTTON, null));
        value.put("複数選択", field(FieldType.MULTI_SELECT, null));
        value.put("複数選択（空）", field(FieldType.MULTI_SELECT, arrayList()));
        value.put("複数選択（null要素）", field(FieldType.MULTI_SELECT, arrayList(null, null)));
        value.put("ドロップダウン", field(FieldType.DROP_DOWN, null));
        value.put("ユーザー選択", field(FieldType.USER_SELECT, null));
        value.put("ユーザー選択（空）", field(FieldType.USER_SELECT, arrayList()));
        value.put("ユーザー選択（null要素）", field(FieldType.USER_SELECT, arrayList(null, null)));
        value.put("ユーザー選択（null項目）", field(FieldType.USER_SELECT, arrayList(member(null, null), member(null, null))));
        value.put("組織選択", field(FieldType.ORGANIZATION_SELECT, null));
        value.put("組織選択（空）", field(FieldType.ORGANIZATION_SELECT, arrayList()));
        value.put("組織選択（null要素）", field(FieldType.ORGANIZATION_SELECT, arrayList(null, null)));
        value.put("組織選択（null項目）", field(FieldType.ORGANIZATION_SELECT, arrayList(member(null, null), member(null, null))));
        value.put("グループ選択", field(FieldType.GROUP_SELECT, null));
        value.put("グループ選択（空）", field(FieldType.GROUP_SELECT, arrayList()));
        value.put("グループ選択（null要素）", field(FieldType.GROUP_SELECT, arrayList(null, null)));
        value.put("グループ選択（null項目）", field(FieldType.GROUP_SELECT, arrayList(member(null, null), member(null, null))));
        value.put("日付", field(FieldType.DATE, null));
        value.put("時刻", field(FieldType.TIME, null));
        value.put("日時", field(FieldType.DATETIME, null));
        value.put("リンク", field(FieldType.LINK, null));
        value.put("添付ファイル", field(FieldType.FILE, null));
        value.put("添付ファイル（空）", field(FieldType.FILE, arrayList()));
        value.put("添付ファイル（null要素）", field(FieldType.FILE, arrayList(null, null)));
        value.put("添付ファイル（null項目）", field(FieldType.FILE, arrayList(file(null, null, null, null), file(null, null, null, null))));
        subTableValueItem.setValue(value);
        return subTableValueItem;
    }

    @SafeVarargs
    private final <T> ArrayList<T> arrayList(final T... a)
    {
        return new ArrayList<>(Arrays.asList(a));
    }

    private Member member(final String code, final String name)
    {
        return new Member(code, name);
    }

    private FileModel file(final String contentType, final String fileKey, final String name, final String size)
    {
        final FileModel file = new FileModel();
        file.setContentType(contentType);
        file.setFileKey(fileKey);
        file.setName(name);
        file.setSize(size);
        return file;
    }

    private FieldValue field(final FieldType type, final Object value)
    {
        final FieldValue field = new FieldValue();
        field.setType(type);
        field.setValue(value);
        return field;
    }
}
