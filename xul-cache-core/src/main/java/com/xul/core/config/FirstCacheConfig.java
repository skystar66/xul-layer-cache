package com.xul.core.config;

import com.xul.core.supports.ExpireMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.TimeUnit;

/**
 * 一级缓存配置
 *
 * @author: xl
 * @date: 2021/9/26
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FirstCacheConfig {

    /**
     * 缓存初始Size
     */
    private int initialCapacity = 10;

    /**
     * 缓存最大Size
     */
    private int maximumSize = 500;

    /**
     * 缓存有效时间
     */
    private int expireTime = 0;

    /**
     * 缓存时间单位
     */
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    /**
     * 缓存失效模式{@link ExpireMode}
     */
    private ExpireMode expireMode = ExpireMode.WRITE;

}
