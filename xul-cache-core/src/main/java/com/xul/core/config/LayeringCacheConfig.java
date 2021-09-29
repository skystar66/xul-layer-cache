package com.xul.core.config;

import lombok.Builder;
import lombok.Data;

/**
 * 多级缓存配置
 *
 * @author: xl
 * @date: 2021/9/26
 **/
@Data
@Builder
public class LayeringCacheConfig {


    private static final String SPLIT = "-";
    /**
     * 内部缓存名 由[一级缓存名-二级缓存名]组成
     */
    private String internalKey;


    /**
     * 一级缓存配置
     */
    private FirstCacheConfig firstCacheConfig;

    /**
     * 二级缓存配置
     */
    private SecondaryCacheConfig secondaryCacheConfig;


    public LayeringCacheConfig(String internalKey, FirstCacheConfig firstCacheConfig, SecondaryCacheConfig secondaryCacheConfig) {
        this.internalKey = internalKey;
        this.firstCacheConfig = firstCacheConfig;
        this.secondaryCacheConfig = secondaryCacheConfig;
        internalKey();
    }

    private void internalKey() {
        // 一级缓存有效时间-二级缓存有效时间
        StringBuilder sb = new StringBuilder();
        if (firstCacheConfig != null) {
            sb.append(firstCacheConfig.getTimeUnit().toMillis(firstCacheConfig.getExpireTime()));
        }
        if (secondaryCacheConfig != null) {
            sb.append(SPLIT);
            sb.append(secondaryCacheConfig.getTimeUnit().toMillis(secondaryCacheConfig.getExpiration()));
        }
        internalKey = sb.toString();
    }

}
