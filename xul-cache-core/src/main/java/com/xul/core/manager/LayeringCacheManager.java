package com.xul.core.manager;

import com.xul.core.cache.Cache;
import com.xul.core.cache.LayeringCache;
import com.xul.core.cache.caffine.CaffeineCache;
import com.xul.core.cache.redis.RedisCache;
import com.xul.core.config.GlobalConfig;
import com.xul.core.config.LayeringCacheConfig;
import com.xul.core.listener.RedisMessageListener;
import com.xul.core.listener.RedisMessagePullTask;
import com.xul.core.redis.client.RedisClient;

/**
 * 多级缓存管理
 *
 * @author: xl
 * @date: 2021/9/28
 **/
public class LayeringCacheManager extends AbstractCacheManager {


    private static class InstanceHolder {
        public static final LayeringCacheManager instance = new LayeringCacheManager();
    }

    public static LayeringCacheManager getInstance() {
        return LayeringCacheManager.InstanceHolder.instance;
    }


    public void init(RedisClient redisClient, String applicationName) throws Exception {
        /**设置缓存命名空间*/
        GlobalConfig.setNamespace(applicationName);
        this.redisClient = redisClient;
        /**redis pub/sub 监听器*/
        RedisMessageListener.getInstance().init(this);
        /**redis pull 消息任务*/
        RedisMessagePullTask.getInstance().init();
        destroy();
    }


    @Override
    protected Cache createCache(String name, LayeringCacheConfig layeringCacheConfig) {
        /**创建一级缓存*/
        CaffeineCache caffeineCache = new CaffeineCache(name, layeringCacheConfig.getFirstCacheConfig());
        /**创建二级缓存*/
        RedisCache redisCache = new RedisCache(name, redisClient, layeringCacheConfig.getSecondaryCacheConfig());

        return new LayeringCache(name, redisClient, caffeineCache, redisCache, layeringCacheConfig);
    }
}
