package com.xul.core.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.xul.core.redis.serializer.SerializationException;
import lombok.NonNull;

import java.io.Reader;
import java.lang.reflect.Type;

public class GSONUtil {

    private static final Gson GSON = new Gson();

    private static final Gson GSON_NULL = new GsonBuilder().serializeNulls().create();

    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();

    private static <T> T innerGet(@NonNull JsonObject jsonObject, String keyName, T defaultValue) {
        JsonElement element = jsonObject.get(keyName);
        if (element == null || element.isJsonNull()) {
            return defaultValue;
        } else {
            return (T) GSON.fromJson(element, defaultValue.getClass());
        }
    }

    public static long get(@NonNull JsonObject jsonObject, String keyName, long defaultValue) {
        Long gt = innerGet(jsonObject, keyName, defaultValue);
        return gt == null ? jsonObject.get(keyName).getAsLong() : gt;
    }

    public static int get(@NonNull JsonObject jsonObject, String keyName, int defaultValue) {
        Integer gt = innerGet(jsonObject, keyName, defaultValue);
        return gt == null ? jsonObject.get(keyName).getAsInt() : gt;
    }

    public static double get(@NonNull JsonObject jsonObject, String keyName, double defaultValue) {
        Double gt = innerGet(jsonObject, keyName, defaultValue);
        return gt == null ? jsonObject.get(keyName).getAsDouble() : gt;
    }

    public static String get(@NonNull JsonObject jsonObject, String keyName, String defaultValue) {
        String gt = innerGet(jsonObject, keyName, defaultValue);
        return gt == null ? jsonObject.get(keyName).getAsString() : gt;
    }

    public static boolean get(@NonNull JsonObject jsonObject, String keyName, boolean defaultValue) {
        Boolean gt = innerGet(jsonObject, keyName, defaultValue);
        return gt == null ? jsonObject.get(keyName).getAsBoolean() : gt;
    }

    public static JsonObject get(@NonNull JsonObject jsonObject, String keyName) {
        JsonElement jsonElement = jsonObject.get(keyName);
        return jsonElement.isJsonNull() ? new JsonObject() : jsonElement.getAsJsonObject();
    }

    public static JsonObject fromJson(@NonNull String json) {
        return GSON.fromJson(json, JsonObject.class);
    }

    public static <T> T fromJson(String json, Class<T> targetType) {
        return GSON.fromJson(json, targetType);
    }

    public static <T> T fromJson(Reader json, Class<T> targetType) {
        return GSON.fromJson(json, targetType);
    }

    public static <T> T fromJson(String json, Type type) {
        return GSON.fromJson(json, type);
    }

    public static <T> T fromJson(JsonElement json, Type type) {
        return GSON.fromJson(json, type);
    }

    public static String toJson(Object object) {
        return GSON.toJson(object);
    }

    public static String toJsonNullable(Object object) {
        return GSON_NULL.toJson(object);
    }

    public static String toJsonPretty(Object object) {
        return GSON_PRETTY.toJson(object);
    }

    public static  <T> byte[] serialize(T obj) throws SerializationException {
        //1. jdk 序列化 //2. json //3.自定义算法（Hessian2）
        return GSON.toJson(obj).getBytes();
    }

    public static <T> T deserialize(byte[] bytes, Class<T> clazz) throws SerializationException {
        return GSON.fromJson(new String(bytes), clazz);//new String(bytes) 用bytes数组构造字符串，每一个byte可以对应一个十进制的数字，由8个bit组成
    }

}
