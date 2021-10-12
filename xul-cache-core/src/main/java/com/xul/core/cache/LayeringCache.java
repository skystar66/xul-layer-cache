package com.xul.core.cache;

import com.xul.core.config.LayeringCacheConfig;
import com.xul.core.exception.LoaderCacheValueException;
import com.xul.core.function.CacheFunctionWithParamReturn;
import com.xul.core.logger.LoggerHelper;
import com.xul.core.redis.client.RedisClient;
import com.xul.core.redis.client.RedissonLockClient;
import com.xul.core.supports.AwaitThreadContainer;
import com.xul.core.utils.GSONUtil;
import com.xul.core.utils.ThreadPoolExecutorTask;
import com.xul.core.supports.NullValue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存
 *
 * @author: xl
 * @date: 2021/9/24
 **/
@Slf4j
public class LayeringCache extends AbstractValueAdaptingCache {


    /**
     * redis客户端
     */
    @Getter
    private final RedisClient redisClient;

    /**
     * 一级缓存
     */
    @Getter
    private final AbstractValueAdaptingCache firstCache;

    /**
     * 二级缓存
     */
    @Getter
    private final AbstractValueAdaptingCache secondCache;

    /**
     * 多级缓存配置
     */
    @Getter
    private final LayeringCacheConfig layeringCacheConfig;

    /**
     * 线程等待容器
     */
    private AwaitThreadContainer awaitThreadContainer = new AwaitThreadContainer();


    public LayeringCache(String cacheName, RedisClient redisClient, AbstractValueAdaptingCache firstCache, AbstractValueAdaptingCache secondCache, LayeringCacheConfig layeringCacheConfig) {
        super(cacheName);
        this.redisClient = redisClient;
        this.firstCache = firstCache;
        this.secondCache = secondCache;
        this.layeringCacheConfig = layeringCacheConfig;
    }

    @Override
    public <T> T get(String key, Class<T> resultType) {
        T result = firstCache.get(key, resultType);
        if (LoggerHelper.isDebugEnabled()) {
            log.info("缓存名称={},查询一级缓存。 key={},返回值是:{}",getCacheName(), key, GSONUtil.toJson(result));
        }
        if (result != null) {
            return (T) fromStoreValue(result);
        }
        result = secondCache.get(key, resultType);
        firstCache.putIfAbsent(key, result, resultType);
        if (LoggerHelper.isDebugEnabled()) {
            log.info("缓存名称={},查询二级缓存,并将数据放到一级缓存。 key={},返回值是:{}",getCacheName(), key, GSONUtil.toJson(result));
        }
        return result;
    }


    @Override
    public <T> T get(String key, Class<T> resultType, CacheFunctionWithParamReturn<T,String> valueLoader) {
        T result = firstCache.get(key, resultType);
        if (LoggerHelper.isDebugEnabled()) {
            log.info("缓存名称={},查询一级缓存。 key={},返回值是:{}",getCacheName(), key, GSONUtil.toJson(result));
        }
        if (result != null) {
            return (T) fromStoreValue(result);
        }

        /**查询二级缓存*/
        result = secondCache.get(key, resultType, valueLoader);
        if (result == null) {
            /**二级缓存为空，获取数据库*/
            result = executeCacheMethod(key, resultType, valueLoader);
        } else {
            /**缓存预刷新*/
            refreshCache(key, resultType, valueLoader, result);
        }
        /**设置一级缓存*/
        firstCache.putIfAbsent(key, result, resultType);
        if (LoggerHelper.isDebugEnabled()) {
            log.info("缓存名称={},查询二级缓存,并将数据放到一级缓存。 key={},返回值是:{}",getCacheName(), key, GSONUtil.toJson(result));
        }
        return result;
    }

    @Override
    public void put(String key, Object value) {
        firstCache.put(key, value);
        secondCache.put(key, value);
        // 更新其它服务器一级缓存
        notifyUpdateFirstCache(key, value, redisClient);


    }

    @Override
    public <T> T putIfAbsent(String key, Object value, Class<T> resultType) {
        T firstResult = firstCache.putIfAbsent(key, value, resultType);
        secondCache.put(key, value);
        // 更新其它服务器一级缓存
        notifyUpdateFirstCache(key, value, redisClient);
        return firstResult;
    }

    @Override
    public void evict(String key) {
        // 删除的时候要先删除二级缓存再删除一级缓存，否则有并发问题
        secondCache.evict(key);
        // 删除其它服务器一级缓存
        notifyDeleteFirstCache(key, redisClient);
    }

    @Override
    public void clear() {
        // 删除的时候要先删除二级缓存再删除一级缓存，否则有并发问题
        secondCache.clear();
        notifyClearFirstCache(redisClient);
    }


    /**
     * 执行缓存方法，查询一级缓存，一级/二级缓存不存在，查询数据库，执行数据加载器
     * 获取锁的线程等待500ms,如果500ms都没返回，则直接释放锁放下一个请求进来，防止第一个线程异常挂掉
     *
     * @param key
     * @param resultType
     * @param valueLoader
     * @return: T
     * @author: xl
     * @date: 2021/9/28
     **/
    private <T> T executeCacheMethod(String key, Class<T> resultType, CacheFunctionWithParamReturn<T,String> valueLoader) {
        while (true) {
            try {
                // 先取缓存，如果有直接返回，没有再去做拿锁操作
                T result = firstCache.get(key, resultType);
                if (result != null) {
                    if (LoggerHelper.isDebugEnabled()) {
                        log.info("缓存名称={},redis缓存 key= {} 获取到锁后查询查询缓存命中，不需要执行被缓存的方法",getCacheName(), key);
                    }
                    return (T) fromStoreValue(result);
                }
                /**获得分布式锁的结果*/
                boolean lockSuccess = redisClient.tryLock(RedissonLockClient.getExecuteDbLockKey(key), layeringCacheConfig.getSecondaryCacheConfig().getWAIT_TIME(),
                        10 * 1000, TimeUnit.MILLISECONDS, () -> {
                            T t = loaderAndPutValue(key, valueLoader);
                            if (LoggerHelper.isDebugEnabled()) {
                                log.info("缓存名称={},redis缓存 key= {} 从数据库获取数据完毕，唤醒所有等待线程",getCacheName(), key);
                            }
                            /**存储到一级缓存中*/
                            firstCache.putIfAbsent(key, t, resultType);
                            // 唤醒线程
                            awaitThreadContainer.signalAll(key);
                            return;
                        });
                /**获取锁失败*/
                if (!lockSuccess) {
                    // 线程等待
                    if (LoggerHelper.isDebugEnabled()) {
                        log.info("缓存名称={},redis缓存 key= {} 从数据库获取数据未获取到锁，进入等待状态，等待{}毫秒",getCacheName(), key, layeringCacheConfig.getSecondaryCacheConfig().getWAIT_TIME());
                    }
                    awaitThreadContainer.await(key, layeringCacheConfig.getSecondaryCacheConfig().getWAIT_TIME());
                }
            } catch (Exception exception) {
                awaitThreadContainer.signalAll(key);
                throw new LoaderCacheValueException(key, exception);
            }
        }

    }


    /**
     * 刷新缓存
     *
     * @param key
     * @param valueLoader value加载器（用于数据库加载，业务端实现函数）
     * @param resultType
     * @param result      返回结果
     * @return: void
     * @author: xl
     * @date: 2021/9/28
     **/
    private <T> void refreshCache(String key, Class<T> resultType, CacheFunctionWithParamReturn<T,String> valueLoader, Object result) {
        ThreadPoolExecutorTask.run(() -> {
            /**缓存主动在失效前强制刷新缓存的时间*/
            long preload = layeringCacheConfig.getSecondaryCacheConfig().getPreloadTime();
            // 允许缓存NULL值，则自动刷新时间也要除以倍数
            boolean flag = (result instanceof NullValue || result == null);
            if (flag) {
                preload = preload / layeringCacheConfig.getSecondaryCacheConfig().getMagnification();
            }
            /**校验是否需要刷新缓存*/
            if (isRefresh(key, preload)) {
                /**判断是否需要强制刷新在开启刷新线程，强制刷新(开启线程，读取mysql数据库，获取新值，软刷新：单线程，重新设置一下redis过期时间，效率高)*/
                if (!layeringCacheConfig.getSecondaryCacheConfig().isForceRefresh()) {
                    /**软刷新*/
                    if (LoggerHelper.isDebugEnabled()) {
                        log.info("缓存名称={},redis缓存 key={} 软刷新缓存模式",getCacheName(), key);
                    }
                    softRefresh(key);
                } else {
                    /**硬刷新*/
                    if (LoggerHelper.isDebugEnabled()) {
                        log.info("缓存名称={},redis缓存 key={} 强刷新缓存模式",getCacheName(), key);
                    }
                    forceRefresh(key, resultType, valueLoader, result);
                }
            }
        });
    }


    /**
     * 软刷新
     *
     * @param key
     * @return: void
     * @author: xl
     * @date: 2021/9/28
     **/
    private void softRefresh(String key) {
        redisClient.tryLock(RedissonLockClient.getTermRedisLockPrefix(key), 100, 50, TimeUnit.MILLISECONDS, () -> {
            redisClient.expire(key, layeringCacheConfig.getSecondaryCacheConfig().getExpiration(), TimeUnit.MILLISECONDS);
        });
    }

    /**
     * 硬刷新(执行查数据库)
     *
     * @param key
     * @param valueLoader 数据加载器
     * @param result      缓存结果
     * @return: void
     * @author: xl
     * @date: 2021/9/28
     **/
    private <T> void forceRefresh(String key, Class<T> resultType, CacheFunctionWithParamReturn<T,String> valueLoader, Object result) {
        redisClient.tryLock(RedissonLockClient.getExecuteDbLockKey(key), 100, 10 * 1000, TimeUnit.MILLISECONDS, () -> {
            try {
                /**查询数据库*/
                Object loadResult = loaderAndPutValue(key, valueLoader);
                if (loadResult != result) {
                    /**更新一级缓存*/
                    //todo 更新其它服务器一级缓存 ，通过 mq
                    firstCache.putIfAbsent(key, loadResult, resultType);
                }
            } catch (Exception exception) {
                log.error("forceRefresh is error:{}", exception);
                throw new LoaderCacheValueException(key, exception);
            }
        });
    }


    /**
     * 加载并将数据放到redis缓存
     *
     * @param key
     * @param valueLoader
     * @return: T
     * @author: xl
     * @date: 2021/9/28
     **/
    private <T> T loaderAndPutValue(String key, CacheFunctionWithParamReturn<T,String> valueLoader) {
        long start = System.currentTimeMillis();
        try {
            // 加载数据
            Object loadResult = valueLoader.invokeMethod(key);
            secondCache.put(key, loadResult);
            if (LoggerHelper.isDebugEnabled()) {
                log.info("缓存名称={},redis缓存 key={} 执行被缓存的方法，并将其放入缓存, 耗时：{}ms。数据:{}",getCacheName(),key, System.currentTimeMillis() - start, GSONUtil.toJson(loadResult));
            }
            return (T) fromStoreValue(loadResult);
        } catch (Exception e) {
            throw new LoaderCacheValueException(key, e);
        }
    }


    /**
     * 判断是否需要刷新缓存
     *
     * @param key
     * @param preloadTime 预加载时间（经过计算后的时间）
     * @return: boolean
     * @author: xl
     * @date: 2021/9/28
     **/
    private boolean isRefresh(String key, long preloadTime) {

        // 获取锁之后再判断一下过期时间，看是否需要加载数据
        Long ttl = redisClient.getExpire(key);
        // -2表示key不存在
        if (ttl == null || ttl == -2) {
            return true;
        }
        // 当前缓存时间小于刷新时间就需要刷新缓存
        return ttl > 0 && TimeUnit.SECONDS.toMillis(ttl) <= preloadTime;
    }


}
