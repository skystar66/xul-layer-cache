package com.xul.core.listener;

import com.xul.core.config.GlobalConfig;
import com.xul.core.logger.LoggerHelper;
import com.xul.core.redis.client.RedisClient;
import com.xul.core.utils.GSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * redis 消息发布者
 *
 * @author: xl
 * @date: 2021/9/28
 **/
@Slf4j
public class RedisPublisher {


    /**
     * 发布消息到频道（Channel）
     *
     * @param redisClient redis客户端
     * @param message     消息内容
     */
    public static void publisher(RedisClient redisClient, RedisPubSubMessage message) {
        publisher(redisClient, message, GlobalConfig.NAMESPACE);
    }

    /**
     * 发布消息到频道（Channel）
     *
     * @param redisClient redis客户端
     * @param message     消息内容
     * @param nameSpace   命名空间
     */
    public static void publisher(RedisClient redisClient, RedisPubSubMessage message, String nameSpace) {
        String messageJson = GSONUtil.toJson(message);
        // pull 拉模式消息
        redisClient.lpush(GlobalConfig.getMessageRedisKey(nameSpace), GlobalConfig.GLOBAL_REDIS_SERIALIZER, messageJson);
        redisClient.expire(GlobalConfig.getMessageRedisKey(nameSpace), 25, TimeUnit.HOURS);
        // pub/sub 推模式消息¬
        redisClient.publish(RedisMessageListener.CHANNEL, "m");
        if (LoggerHelper.isDebugEnabled()) {
            log.info("redis消息发布者向频道【{}】发布了【{}】消息", RedisMessageListener.CHANNEL, message.toString());
        }
    }


}
