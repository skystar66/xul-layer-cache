package com.xul.core.manager;

import com.xul.core.cache.Cache;
import com.xul.core.config.LayeringCacheConfig;

import java.util.Collection;

/**
 * 缓存管理器
 *
 * @author: xl
 * @date: 2021/9/28
 **/
public interface CacheManager {

    /**
     * 根据缓存名称返回对应的{@link Collection}.
     *
     * @param name 缓存的名称 (不能为 {@code null})
     * @return 返回对应名称的Cache, 如果没找到返回 {@code null}
     */
    Cache getCache(String name);

    /**
     * 根据缓存名称返回对应的{@link Cache}，如果没有找到就新建一个并放到容器
     *
     * @param name                缓存名称
     * @param layeringCacheConfig 多级缓存配置
     * @return {@link Cache}
     */
    Cache getCache(String name, LayeringCacheConfig layeringCacheConfig);

    /**
     * 获取所有缓存名称的集合
     *
     * @return 所有缓存名称的集合
     */
    Collection<String> getCacheNames();


}
