package com.xul.core.cache.caffine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xul.core.cache.AbstractValueAdaptingCache;
import com.xul.core.config.FirstCacheConfig;
import com.xul.core.exception.LoaderCacheValueException;
import com.xul.core.function.CacheFunctionWithParamReturn;
import com.xul.core.logger.LoggerHelper;
import com.xul.core.supports.ExpireMode;
import com.xul.core.utils.GSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

/**
 * 基于caffeine实现的一级缓存
 *
 * @author: xl
 * @date: 2021/9/26
 **/
@Slf4j
public class CaffeineCache extends AbstractValueAdaptingCache {


    private final Cache<Object, Object> cache;


    public CaffeineCache(String cacheName, FirstCacheConfig firstCacheConfig) {
        super(cacheName);
        this.cache = getCache(firstCacheConfig);
    }

    /**
     * 构造一级缓存对象
     *
     * @param firstCacheConfig 一级缓存配置
     * @return: com.github.benmanes.caffeine.cache.Cache<java.lang.Object, java.lang.Object>
     * @author: xl
     * @date: 2021/9/26
     **/
    private Cache<Object, Object> getCache(FirstCacheConfig firstCacheConfig) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        builder.initialCapacity(firstCacheConfig.getInitialCapacity());
        builder.maximumSize(firstCacheConfig.getMaximumSize());
        /**软引用，内存不够时gc回收*/
        builder.softValues();
        if (ExpireMode.WRITE.equals(firstCacheConfig.getExpireMode())) {
            builder.expireAfterWrite(firstCacheConfig.getExpireTime(), firstCacheConfig.getTimeUnit());
        } else if (ExpireMode.ACCESS.equals(firstCacheConfig.getExpireMode())) {
            builder.expireAfterAccess(firstCacheConfig.getExpireTime(), firstCacheConfig.getTimeUnit());
        }
        return builder.build();
    }

    @Override
    public <T> T get(String key, Class<T> resultType) {
        Object result = cache.getIfPresent(key);
        if (result != null) {
            if (LoggerHelper.isDebugEnabled()) {
                log.info("caffine 获取缓存 key={},result={}", key, result);
            }
            return (T) result;
        }
        return null;
    }


    @Override
    public <T> T get(String key, Class<T> resultType, CacheFunctionWithParamReturn<T, String> valueLoader) {
        Object result = this.cache.get(key, k -> loaderValue(key, valueLoader));

        return (T) fromStoreValue(result);
    }


    @Override
    public void put(String key, Object value) {
        cache.put(key, value);

    }

    @Override
    public <T> T putIfAbsent(String key, Object value, Class<T> resultType) {
        Object result = cache.get(key, k -> value);
        return (T) result;
    }

    @Override
    public void evict(String key) {
        cache.invalidate(key);
        if (LoggerHelper.isDebugEnabled()) {
            log.info("caffine 移除缓存 key={}", key);
        }
    }

    @Override
    public void clear() {
        if (LoggerHelper.isDebugEnabled()) {
            log.info("caffine 清空缓存");
        }
        cache.invalidateAll();
    }

    /**
     * 加载数据
     */
    private <T> Object loaderValue(String key, CacheFunctionWithParamReturn<T, String> valueLoader) {
        try {
            T t = valueLoader.invokeMethod(key);
            if (LoggerHelper.isDebugEnabled()) {
                log.info("caffeine缓存 key={} 从库加载缓存", key, GSONUtil.toJson(t));
            }
            return toStoreValue(t);
        } catch (Exception e) {
            throw new LoaderCacheValueException(key, e);
        }

    }

}
