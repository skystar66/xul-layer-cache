package com.xul.cache.starter.config;

import com.xul.core.config.RedisConfig;
import com.xul.core.manager.LayeringCacheManager;
import com.xul.core.redis.client.RedisClient;
import com.xul.core.redis.serializer.RedisSerializer;
import com.xul.core.utils.FileUtil;
import com.xul.core.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.Map;

/**
 * 分布式缓存自动配置
 * BeanPostProcessor在spring初始化bean之前 执行postProcessBeforeInitialization方法
 *
 * @author: xl
 * @date: 2021/9/29
 * @see BeanPostProcessor#postProcessBeforeInitialization(Object, String)
 **/
@Slf4j
public class LayeringCacheAutoConfig implements BeanPostProcessor {

    static Map<String, Object> configAllMap = FileUtil.loadFlattenedYaml();

    private static boolean isFlag = false;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (isFlag) {
            return bean;
        }
        String applicationName = configAllMap.getOrDefault("spring.application.name", "").toString();
        RedisConfig redisConfig = new RedisConfig();
        redisConfig.setDatabase(Integer.parseInt(configAllMap.getOrDefault("layering-cache.redis.databse", 0).toString()));
        redisConfig.setHost(configAllMap.getOrDefault("layering-cache.redis.host", "127.0.0.1").toString());
        redisConfig.setCluster(configAllMap.getOrDefault("layering-cache.redis.cluster", "").toString());
        redisConfig.setPassword(StringUtils.isBlank(configAllMap.getOrDefault("layering-cache.redis.password", "").toString())
                ? null : configAllMap.getOrDefault("layering-cache.redis.password", "").toString());
        redisConfig.setPort(Integer.parseInt(configAllMap.getOrDefault("layering-cache.redis.port", 6379).toString()));
        redisConfig.setKeySerializer(configAllMap.getOrDefault("layering-cache.redis.key-serializer", "com.xul.core.redis.serializer.StringRedisSerializer").toString());
        redisConfig.setValueSerializer(configAllMap.getOrDefault("layering-cache.redis.value-serializer", "com.xul.core.redis.serializer.ProtostuffRedisSerializer").toString());
        redisConfig.setIdleMaxSize(Integer.parseInt(configAllMap.getOrDefault("layering-cache.redis.idlemax-size", 32).toString()));
        redisConfig.setMaxSize(Integer.parseInt(configAllMap.getOrDefault("layering-cache.redis.max-size", 64).toString()));
        redisConfig.setMinSize(Integer.parseInt(configAllMap.getOrDefault("layering-cache.redis.min-size", 10).toString()));
        try {
            RedisSerializer valueRedisSerializer = (RedisSerializer) Class.forName(redisConfig.getValueSerializer()).newInstance();
            RedisSerializer keyRedisSerializer = (RedisSerializer) Class.forName(redisConfig.getKeySerializer()).newInstance();
            RedisClient redisClient = RedisClient.getInstance(redisConfig);
            redisClient.setKeySerializer(keyRedisSerializer);
            redisClient.setValueSerializer(valueRedisSerializer);
            /**初始化分布式缓存管理器*/
            LayeringCacheManager.getInstance().init(redisClient, applicationName);
            log.info(">>>>>>>>>> layering-cache init success config=[{}]!!! <<<<<<<<<<",redisConfig);
        } catch (Exception exception) {
            log.error("初始化分布式缓存发生错误！{}", exception);
        }
        isFlag = true;
        return bean;
    }
}
