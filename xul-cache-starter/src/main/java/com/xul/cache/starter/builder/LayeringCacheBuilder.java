package com.xul.cache.starter.builder;

import com.xul.core.cache.Cache;
import com.xul.core.config.FirstCacheConfig;
import com.xul.core.config.LayeringCacheConfig;
import com.xul.core.config.RedisConfig;
import com.xul.core.config.SecondaryCacheConfig;
import com.xul.core.manager.LayeringCacheManager;
import com.xul.core.redis.client.RedisClient;
import com.xul.core.redis.serializer.RedisSerializer;
import com.xul.core.utils.FileUtil;
import com.xul.core.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 分布式缓存构造器
 *
 * @author: xl
 * @date: 2021/9/29
 **/
@Slf4j
public class LayeringCacheBuilder {

    /**
     * 缓存名称
     */
    private String cacheName;

    /**
     * 一级缓存配置
     */
    private FirstCacheConfig firstCacheConfig;

    /**
     * 二级缓存配置
     */
    private SecondaryCacheConfig secondaryCacheConfig;


    /**
     * 提供一个静态builder方法
     */
    public static LayeringCacheBuilder.Builder builder() {
        return new LayeringCacheBuilder.Builder();
    }


    static Map<String, Object> configAllMap = FileUtil.loadFlattenedYaml();

    static {
        String applicationName = configAllMap.getOrDefault("spring.application.name", "").toString();
        RedisConfig redisConfig = new RedisConfig();
        redisConfig.setDatabase(Integer.parseInt(configAllMap.getOrDefault("layering-cache.redis.databse", 0).toString()));
        redisConfig.setHost(configAllMap.getOrDefault("layering-cache.redis.host", "127.0.0.1").toString());
        redisConfig.setCluster(configAllMap.getOrDefault("layering-cache.redis.cluster", "").toString());
        redisConfig.setPassword(StringUtils.isBlank(configAllMap.getOrDefault("layering-cache.redis.password", "").toString())
                ? null : configAllMap.getOrDefault("layering-cache.redis.password", "").toString());
        redisConfig.setPort(Integer.parseInt(configAllMap.getOrDefault("layering-cache.redis.port", 6379).toString()));
        redisConfig.setSerializer("com.xul.core.redis.serializer.KryoRedisSerializer");
        try {
            RedisSerializer valueRedisSerializer = (RedisSerializer) Class.forName(redisConfig.getSerializer()).newInstance();
            RedisSerializer keyRedisSerializer = (RedisSerializer) Class.forName(redisConfig.getSerializer()).newInstance();
            RedisClient redisClient = RedisClient.getInstance(redisConfig);
            redisClient.setKeySerializer(keyRedisSerializer);
            redisClient.setValueSerializer(valueRedisSerializer);
            /**初始化分布式缓存管理器*/
            LayeringCacheManager.getInstance().init(redisClient, applicationName);
            log.info("layering-cache init success!!!");
        } catch (Exception exception) {
            log.error("初始化分布式缓存发生错误！{}", exception);
        }
    }

    public static class Builder {
        /**
         * 缓存名称
         */
        private String cacheName;

        /**
         * 一级缓存配置
         */
        private FirstCacheConfig firstCacheConfig;

        /**
         * 二级缓存配置
         */
        private SecondaryCacheConfig secondaryCacheConfig;

        public Builder cacheName(String cacheName) {
            this.cacheName = cacheName;
            return this;
        }

        public Builder firstCacheConfig(FirstCacheConfig firstCacheConfig) {
            this.firstCacheConfig = firstCacheConfig;
            return this;
        }

        public Builder secondaryCacheConfig(SecondaryCacheConfig secondaryCacheConfig) {
            this.secondaryCacheConfig = secondaryCacheConfig;
            return this;
        }

        //获取缓存
        public Cache build() {
            return new LayeringCacheBuilder(this).build();
        }
    }

    private LayeringCacheBuilder(Builder builder) {
        cacheName = builder.cacheName;
        firstCacheConfig = builder.firstCacheConfig;
        secondaryCacheConfig = builder.secondaryCacheConfig;
    }


    /**
     * 获取缓存
     */
    public Cache build() {
        /**构造分布式缓存*/
        return LayeringCacheManager.getInstance().getCache(this.cacheName
                , LayeringCacheConfig.builder()
                        .firstCacheConfig(this.firstCacheConfig)
                        .secondaryCacheConfig(this.secondaryCacheConfig)
                        .build()
        );
    }


}
