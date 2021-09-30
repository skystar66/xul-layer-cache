package com.xul.cache.starter.properties;


import com.xul.core.utils.StringUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
//@ConfigurationProperties(prefix = "layering-cache.redis")
public class LayeringCacheRedisProperties {
    Integer database = 0;
    /**
     * 不为空表示集群版，示例
     * localhost:7379,localhost2:7379
     */
    String cluster = "";
    String host = "localhost";
    Integer port = 6379;
    String password = null;
    /**
     * 序列化方式:
     * com.xul.core.redis.serializer.KryoRedisSerializer
     * com.xul.core.redis.serializer.FastJsonRedisSerializer
     * com.xul.core.redis.serializer.JacksonRedisSerializer
     * com.xul.core.redis.serializer.JdkRedisSerializer
     * com.xul.core.redis.serializer.ProtostuffRedisSerializer
     */
    String serializer = "com.xul.core.redis.serializer.KryoRedisSerializer";

    public String getPassword() {
        return StringUtils.isBlank(password) ? null : password;
    }
}