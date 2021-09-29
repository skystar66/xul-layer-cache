package com.xul.core.manager;

import com.xul.core.cache.Cache;
import com.xul.core.config.LayeringCacheConfig;
import com.xul.core.listener.RedisMessageListener;
import com.xul.core.listener.RedisMessagePullTask;
import com.xul.core.redis.client.RedisClient;
import com.xul.core.utils.ThreadPoolExecutorTask;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 公共的抽象实现 {@link CacheManager} 的实现.
 *
 * @author: xl
 * @date: 2021/9/28
 **/
@Slf4j
public abstract class AbstractCacheManager implements CacheManager {


    /**
     * 缓存容器
     * 外层key是cache_name
     */
    @Getter
    private final ConcurrentMap<String, Cache> cacheContainer = new ConcurrentHashMap<>(16);

    /**
     * 缓存名称容器
     */
    @Getter
    private CopyOnWriteArraySet<String> cacheNames = new CopyOnWriteArraySet<>();

    /**
     * redis 客户端
     */
    @Getter
    RedisClient redisClient;



    @Override
    public Cache getCache(String name) {
        Cache cache = this.cacheContainer.get(name);
        return cache;
    }

    @Override
    public Cache getCache(String name, LayeringCacheConfig layeringCacheConfig) {

        // 第一次获取缓存Cache，如果有直接返回,如果没有加锁往容器里里面放Cache
        Cache cache = this.cacheContainer.get(name);
        if (null != cache) {
            return cache;
        }
        // 第二次获取缓存Cache，加锁往容器里里面放Cache
        synchronized (this.cacheContainer) {
            cache = this.cacheContainer.get(name);
            if (null != cache) {
                return cache;
            }
            // 新建一个Cache对象
            cache = createCache(name, layeringCacheConfig);
            cacheNames.add(name);
            if (cache != null) {
                // 将新的Cache对象放到容器
                this.cacheContainer.put(name, cache);
            }
            return cache;
        }
    }

    @Override
    public Collection<String> getCacheNames() {
        return cacheNames;
    }


    public void destroy() throws Exception {

        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            ThreadPoolExecutorTask.close();
        }));


    }



    /**
     * 根据缓存名称在CacheManager中没有找到对应Cache时，通过该方法新建一个对应的Cache实例
     *
     * @param name                缓存名称
     * @param layeringCacheConfig 缓存配置
     * @return {@link Cache}
     */
    protected abstract Cache createCache(String name, LayeringCacheConfig layeringCacheConfig);
}
