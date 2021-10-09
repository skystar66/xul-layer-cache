package com.xul.cache.web.controller;


import com.xul.cache.starter.builder.LayeringCacheBuilder;
import com.xul.cache.web.cache.XulLayeringCache;
import com.xul.core.cache.Cache;
import com.xul.core.config.FirstCacheConfig;
import com.xul.core.config.SecondaryCacheConfig;
import com.xul.core.supports.ExpireMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/cache/")
@Slf4j
public class CacheController {

    /**
     * 使用方式：demo
     * 构建一个订单分布式缓存
     */
    private static Cache orderCache = LayeringCacheBuilder.builder()
            /**缓存名称*/
            .cacheName("order")
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
        XulLayeringCache.getUserCache().put(key, value);

        return "success";
    }


    @RequestMapping("get")
    public String get(@RequestParam("key") String key) {
        orderCache.get(key, String.class);
        return XulLayeringCache.getUserCache().get(key, String.class);
    }


    @RequestMapping("getIfAbsent")
    public String getIfAbsent(@RequestParam("key") String key) {
        orderCache.get(key, String.class);
        return XulLayeringCache.getUserCache().get(key, String.class, this::getData);
    }


    @RequestMapping("del")
    public String del(@RequestParam("key") String key) {
        XulLayeringCache.getUserCache().evict(key);
        return "success";
    }


    public String getData(String key) {
        log.info("从数据库中 获取数据，key={}", key);
        return key + ":db-data";
    }


}
