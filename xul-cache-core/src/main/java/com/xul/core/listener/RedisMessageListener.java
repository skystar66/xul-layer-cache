package com.xul.core.listener;

import com.xul.core.logger.LoggerHelper;
import com.xul.core.manager.AbstractCacheManager;
import io.lettuce.core.pubsub.RedisPubSubListener;
import lombok.extern.slf4j.Slf4j;

/**
 * redis消息订阅者
 *
 * @author: xl
 * @date: 2021/9/27
 **/
@Slf4j
public class RedisMessageListener implements RedisPubSubListener<String, String> {

    /**
     * 消息通道
     */
    public static final String CHANNEL = "layering-cache-channel";

    /**
     * redis消息处理器
     */
    private RedisMessageService redisMessageService;


    private static class InstanceHolder {
        public static final RedisMessageListener instance = new RedisMessageListener();
    }

    public static RedisMessageListener getInstance() {
        return RedisMessageListener.InstanceHolder.instance;
    }

    public void init(AbstractCacheManager cacheManager) {
        redisMessageService = RedisMessageService.getInstance().init(cacheManager);
        // 创建监听
        cacheManager.getRedisClient().subscribe(this, RedisMessageListener.CHANNEL);
    }

    @Override
    public void message(String channel, String message) {
        try {
            if (LoggerHelper.isDebugEnabled()) {
                log.info("redis消息订阅者接收到频道【{}】发布的消息。消息内容：{}", channel, message);
            }
            // 更新最后一次处理推消息的时间
            redisMessageService.updateLastPushTime();

            /**拉取消息*/
            redisMessageService.pullMessage();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("layering-cache 清除一级缓存异常：{}", e.getMessage(), e);
        }
    }

    @Override
    public void message(String pattern, String channel, String message) {

    }

    @Override
    public void subscribed(String channel, long count) {

    }

    @Override
    public void psubscribed(String pattern, long count) {

    }

    @Override
    public void unsubscribed(String channel, long count) {

    }

    @Override
    public void punsubscribed(String pattern, long count) {

    }
}
