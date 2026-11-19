package io.github.itech_framework.code_editor.demo;

import io.github.itech_framework.code_editor.internal.json.JsonException;
import io.github.itech_framework.code_editor.internal.json.JsonReader;

public final class JsonReaderCheck {

    public static void main(String[] args) {
        System.out.println(JsonReader.parse(
                "{\"version\":1,\"name\":\"a\\u0041\",\"list\":[1,2.5,true,null],\"nested\":{\"x\":-3e2}}"));
        fail("{\"a\":1,}");
        fail("{\"a\":1,\"a\":2}");
        fail("{\n  \"a\": tru\n}");
        fail("[1 2]");
        fail("\"abc");
    }

    private static void fail(String text) {
        try {
            JsonReader.parse(text);
            System.out.println("NO ERROR");
        } catch (JsonException ex) {
            System.out.println(ex.getMessage());
        }
    }
}