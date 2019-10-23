package org.embulk.input.kintone;

import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.embulk.config.ConfigSource;
import org.embulk.test.TestingEmbulk;

import java.io.FileReader;
import java.io.IOException;

public class TestHelpers {

    private TestHelpers() {}

    public static JsonObject getJsonFromFile(String fileName) throws IOException{
        String path = Resources.getResource(fileName).getPath();
        try (JsonReader reader = new JsonReader(new FileReader(path))) {
            JsonParser parser = new JsonParser();
            return parser.parse(reader).getAsJsonObject();
        }
    }

    public static ConfigSource loadYamlResource(TestingEmbulk embulk, String fileName) {
        String path = Resources.getResource(fileName).getPath();
        return embulk.loadYamlResource(path);
    }

    public static JsonElement jsonObj2JsonElement(JsonObject jobj){
        Gson gson = new Gson();
        return gson.fromJson(jobj.toString(), JsonElement.class);
    }
}
