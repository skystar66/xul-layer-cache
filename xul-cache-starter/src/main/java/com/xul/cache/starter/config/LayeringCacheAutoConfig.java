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
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 分布式缓存自动配置
 *
 * @author: xl
 * @date: 2021/9/29
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
        redisConfig.setSerializer("com.xul.core.redis.serializer.KryoRedisSerializer");
        try {
            RedisSerializer valueRedisSerializer = (RedisSerializer) Class.forName(redisConfig.getSerializer()).newInstance();
            RedisSerializer keyRedisSerializer = (RedisSerializer) Class.forName(redisConfig.getSerializer()).newInstance();
            RedisClient redisClient = RedisClient.getInstance(redisConfig);
            redisClient.setKeySerializer(keyRedisSerializer);
            redisClient.setValueSerializer(valueRedisSerializer);
            /**初始化分布式缓存管理器*/
            LayeringCacheManager.getInstance().init(redisClient, applicationName);
            log.info(">>>>>>>>>> layering-cache init success!!! <<<<<<<<<<");
        } catch (Exception exception) {
            log.error("初始化分布式缓存发生错误！{}", exception);
        }
        isFlag = true;
        return bean;
    }
}
