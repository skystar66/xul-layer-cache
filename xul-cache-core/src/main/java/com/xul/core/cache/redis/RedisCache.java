package com.xul.core.cache.redis;

import com.xul.core.cache.AbstractValueAdaptingCache;
import com.xul.core.config.SecondaryCacheConfig;
import com.xul.core.function.CacheFunctionWithParamReturn;
import com.xul.core.redis.client.RedisClient;
import com.xul.core.supports.NullValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 基于redis实现的二级缓存
 *
 * @author: xl
 * @date: 2021/9/26
 **/
@Slf4j
public class RedisCache extends AbstractValueAdaptingCache {

    /**
     * 缓存有效时间,毫秒
     */
    private final long expiration;


    private final RedisClient redisClient;


    private final TimeUnit timeUnit;

    /**
     * 非空值和null值之间的时间倍率，默认是1。allowNullValue=true才有效
     * <p>
     * 如配置缓存的有效时间是200秒，倍率这设置成10，
     * 那么当缓存value为null时，缓存的有效时间将是20秒，非空时为200秒
     * </p>
     */
    int magnification = 1;

    public RedisCache(String name, RedisClient redisClient, SecondaryCacheConfig secondaryCacheConfig) {
        this(name, redisClient, secondaryCacheConfig.getExpiration(), secondaryCacheConfig.getPreloadTime(), secondaryCacheConfig.isForceRefresh(),
                secondaryCacheConfig.getTimeUnit());
    }


    /**
     * RedisCache
     *
     * @param cacheName    缓存名称
     * @param redisClient  redis客户端
     * @param expiration   key的有效时间
     * @param preloadTime  缓存主动在失效前强制刷新缓存的时间
     * @param forceRefresh 是否强制刷新（执行被缓存的方法），默认是false
     * @return:
     * @author: xl
     * @date: 2021/9/28
     **/
    public RedisCache(String cacheName, RedisClient redisClient, long expiration, long preloadTime, boolean forceRefresh, TimeUnit timeUnit) {
        super(cacheName);
        this.redisClient = redisClient;
        this.expiration = expiration;
        this.timeUnit = timeUnit;
    }

    @Override
    public <T> T get(String key, Class<T> resultType) {
        return redisClient.get(key, resultType);
    }


    @Override
    public <T> T get(String key, Class<T> resultType, CacheFunctionWithParamReturn<T, String> valueLoader) {
        // 先获取缓存，如果有直接返回
        T result = redisClient.get(key, resultType);
        if (result != null) {
            return (T) fromStoreValue(result);
        }
        return null;
    }


    @Override
    public void put(String key, Object value) {
        putValue(key, value);
    }

    @Override
    public <T> T putIfAbsent(String key, Object value, Class<T> resultType) {
        T result = get(key, resultType);
        if (result != null) {
            return result;
        }
        put(key, value);
        return null;
    }

    @Override
    public void evict(String key) {

        redisClient.delete(key);
    }

    @Override
    public void clear() {
        /**清理掉前缀的key value*/
        Set<String> keys = redisClient.scan(getCacheName() + "*");
        if (!CollectionUtils.isEmpty(keys)) {
            redisClient.delete(keys);
        }
    }

    private void putValue(String key, Object value) {
        Object result = toStoreValue(value);
        // 允许缓存NULL值
        long expirationTime = this.expiration;
        // 允许缓存NULL值且缓存为值为null时需要重新计算缓存时间
        if (value instanceof NullValue) {
            expirationTime = expirationTime / magnification;
        }
        // 将数据放到缓存
        redisClient.set(key, result, expirationTime, timeUnit);
    }


}
