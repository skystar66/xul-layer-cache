package com.xul.cache.starter.builder;

import com.xul.core.cache.Cache;
import com.xul.core.config.FirstCacheConfig;
import com.xul.core.config.LayeringCacheConfig;
import com.xul.core.config.SecondaryCacheConfig;
import com.xul.core.manager.LayeringCacheManager;
import lombok.extern.slf4j.Slf4j;

/**
 * 分布式缓存构造器
 *
 * @author: xl
 * @date: 2021/9/29
 **/
@Slf4j
public class LayeringCacheBuilder {

    /**
     * 缓存名称
     */
    private String cacheName;

    /**
     * 一级缓存配置
     */
    private FirstCacheConfig firstCacheConfig;

    /**
     * 二级缓存配置
     */
    private SecondaryCacheConfig secondaryCacheConfig;

    /**
     * 提供一个静态builder方法
     */
    public static LayeringCacheBuilder.Builder builder() {
        return new LayeringCacheBuilder.Builder();
    }

    public static class Builder {
        /**
         * 缓存名称
         */
        private String cacheName;

        /**
         * 一级缓存配置
         */
        private FirstCacheConfig firstCacheConfig;

        /**
         * 二级缓存配置
         */
        private SecondaryCacheConfig secondaryCacheConfig;

        public Builder cacheName(String cacheName) {
            this.cacheName = cacheName;
            return this;
        }

        public Builder firstCacheConfig(FirstCacheConfig firstCacheConfig) {
            this.firstCacheConfig = firstCacheConfig;
            return this;
        }

        public Builder secondaryCacheConfig(SecondaryCacheConfig secondaryCacheConfig) {
            this.secondaryCacheConfig = secondaryCacheConfig;
            return this;
        }

        /**
         * 构造缓存
         */
        public Cache build() {
            return new LayeringCacheBuilder(this).build();
        }
    }

    private LayeringCacheBuilder(Builder builder) {
        cacheName = builder.cacheName;
        firstCacheConfig = builder.firstCacheConfig;
        secondaryCacheConfig = builder.secondaryCacheConfig;
    }


    /**
     * 获取缓存
     */
    public Cache build() {
        /**构造分布式缓存*/
        return LayeringCacheManager.getInstance().getCache(this.cacheName
                , LayeringCacheConfig.builder()
                        .firstCacheConfig(this.firstCacheConfig)
                        .secondaryCacheConfig(this.secondaryCacheConfig)
                        .build()
        );
    }


}
