package com.xul.cache.web.cache;


import com.xul.cache.starter.builder.LayeringCacheBuilder;
import com.xul.core.cache.Cache;
import com.xul.core.config.FirstCacheConfig;
import com.xul.core.config.SecondaryCacheConfig;
import com.xul.core.supports.ExpireMode;
import lombok.Getter;

import java.util.concurrent.TimeUnit;

public class XulLayeringCache {
    /**
     * 使用方式：demo
     * 构建一个用户分布式缓存
     */
    @Getter
    private static Cache userCache = LayeringCacheBuilder.builder()
            /**缓存名称*/
            .cacheName("user")
            /**一级缓存配置*/
            .firstCacheConfig(FirstCacheConfig.builder()
                    .maximumSize(1000000)
                    .expireMode(ExpireMode.WRITE)
                    .initialCapacity(10000)
                    .expireTime(1000)
                    .timeUnit(TimeUnit.SECONDS)
                    .build())
            /**二级缓存配置*/
            .secondaryCacheConfig(SecondaryCacheConfig.builder()
                    .expiration(100 * 60)
                    .magnification(10)
                    .preloadTime(30)
                    .timeUnit(TimeUnit.SECONDS)
                    .forceRefresh(true)
                    .build())
            .build();
}
