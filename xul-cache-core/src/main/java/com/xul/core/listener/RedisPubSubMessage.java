package com.xul.core.listener;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * redis  pub/sub 消息
 *
 * @author: xl
 * @date: 2021/9/28
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RedisPubSubMessage {


    /**
     * 缓存名称
     */
    private String cacheName;

    /**
     * 缓存key
     */
    private String key;

    /**
     * 缓存 value
     */
    private Object value;

    /**
     * 消息类型
     */
    private RedisPubSubMessageType messageType;


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RedisPubSubMessage{");
        sb.append("cacheName='").append(cacheName).append('\'');
        sb.append(", key=").append(key);
        sb.append(", value=").append(value);
        sb.append(", messageType=").append(messageType);
        sb.append('}');
        return sb.toString();
    }
}
