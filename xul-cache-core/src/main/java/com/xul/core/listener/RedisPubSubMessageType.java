package com.xul.core.listener;

/**
 * 消息类型
 *
 * @author xl
 */
public enum RedisPubSubMessageType {
    /**
     * 删除缓存
     */
    EVICT("删除缓存"),


    /**
     * 更新缓存
     */
    UPDATE("更新缓存"),

    /**
     * 清空缓存
     */
    CLEAR("清空缓存");

    private String label;

    RedisPubSubMessageType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}