package com.xul.core.cache;

import com.xul.core.redis.client.RedisClient;
import com.xul.core.supports.NullValue;
import com.xul.core.listener.RedisPubSubMessage;
import com.xul.core.listener.RedisPubSubMessageType;
import com.xul.core.listener.RedisPublisher;

/**
 * cache抽象类，对公共方法进行抽象提取实现
 *
 * @author: xl
 * @date: 2021/9/26
 **/
public abstract class AbstractValueAdaptingCache implements Cache {


    /**
     * 缓存名称
     **/
    private final String cacheName;

    /**
     * 设置缓存名称
     *
     * @param cacheName
     * @author: xl
     * @date: 2021/9/26
     **/
    protected AbstractValueAdaptingCache(String cacheName) {
        this.cacheName = cacheName;
    }


    @Override
    public String getCacheName() {
        return this.cacheName;
    }

    protected Object toStoreValue(Object userValue) {
        if (userValue == null) {
            return NullValue.INSTANCE;
        }
        return userValue;
    }

    protected Object fromStoreValue(Object storeValue) {
        if (storeValue instanceof NullValue) {
            return null;
        }
        return storeValue;
    }


    /**
     * 通知更新一级缓存消息
     *
     * @param key
     * @param value
     * @param redisClient
     * @return: void
     * @author: xl
     * @date: 2021/9/28
     **/
    public void notifyUpdateFirstCache(String key, Object value, RedisClient redisClient) {
        // 更新一级缓存需要用到redis的Pub/Sub（订阅/发布）模式，否则集群中其他服服务器节点的一级缓存数据无法更新

        RedisPubSubMessage message = RedisPubSubMessage.builder()
                .cacheName(cacheName)
                .key(key)
                .value(value)
                .messageType(RedisPubSubMessageType.UPDATE)
                .build();
        // 发布消息
        RedisPublisher.publisher(redisClient, message);
    }

    /**
     * 通知删除一级缓存消息
     *
     * @param key
     * @param redisClient
     * @return: void
     * @author: xl
     * @date: 2021/9/28
     **/
    public void notifyDeleteFirstCache(String key, RedisClient redisClient) {
        // 删除一级缓存需要用到redis的Pub/Sub（订阅/发布）模式，否则集群中其他服服务器节点的一级缓存数据无法删除
        RedisPubSubMessage message = RedisPubSubMessage.builder()
                .cacheName(cacheName)
                .key(key)
                .messageType(RedisPubSubMessageType.EVICT)
                .build();
        // 发布消息
        RedisPublisher.publisher(redisClient, message);
    }

    /**
     * 清理缓存
     *
     * @param redisClient
     * @return: void
     * @author: xl
     * @date: 2021/9/28
     **/
    public void notifyClearFirstCache(RedisClient redisClient) {
        // 清理一级缓存需要用到redis的Pub/Sub（订阅/发布）模式，否则集群中其他服服务器节点的一级缓存数据无法清理
        RedisPubSubMessage message = RedisPubSubMessage.builder()
                .cacheName(cacheName)
                .messageType(RedisPubSubMessageType.CLEAR)
                .build();
        // 发布消息
        RedisPublisher.publisher(redisClient, message);
    }


}
