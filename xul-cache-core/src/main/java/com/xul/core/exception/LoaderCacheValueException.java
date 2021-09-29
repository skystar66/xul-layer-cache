package com.xul.core.exception;

public class LoaderCacheValueException extends RuntimeException {

    private final Object key;

    public LoaderCacheValueException(String key, Throwable ex) {
        super(String.format("加载key为 %s 的缓存数据,执行被缓存方法异常",key), ex);
        this.key = key;
    }

    public Object getKey() {
        return this.key;
    }

}
