package com.xul.cache.web.controller;


import com.xul.cache.starter.builder.LayeringCacheBuilder;
import com.xul.core.cache.Cache;
import com.xul.core.config.FirstCacheConfig;
import com.xul.core.config.SecondaryCacheConfig;
import com.xul.core.supports.ExpireMode;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/cache/")
public class CacheController {

    /**
     * 使用方式：demo
     * 构建一个用户分布式缓存
     */
    private Cache userCache = LayeringCacheBuilder.builder()
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

    @RequestMapping("put")
    public String put(@RequestParam("key") String key, @RequestParam("value") String value) {
        userCache.put(key, value);
        return "success";
    }


    @RequestMapping("get")
    public String get(@RequestParam("key") String key) {
        return userCache.get(key, String.class);
    }


    @RequestMapping("del")
    public String del(@RequestParam("key") String key) {
        userCache.evict(key);
        return "success";
    }


}
