package com.xul.core.config;

import lombok.Data;

@Data
public class RedisConfig {

    /**
     * 不为空表示集群版，示例
     * localhost:7379,localhost2:7379
     */
    private String cluster = "";

    private String host;


    private int port;

    private String password;

    private int database;


    private int maxSize=500;
    private int idleMaxSize=100;
    private int minSize=100;

    /**
     * 序列化方式:
     * com.xul.core.redis.serializer.KryoRedisSerializer
     * com.xul.core.redis.serializer.FastJsonRedisSerializer
     * com.xul.core.redis.serializer.JacksonRedisSerializer
     * com.xul.core.redis.serializer.JdkRedisSerializer
     * com.xul.core.redis.serializer.ProtostuffRedisSerializer
     */
    String keySerializer = "com.xul.core.redis.serializer.StringRedisSerializer";
    String valueSerializer = "com.xul.core.redis.serializer.ProtostuffRedisSerializer";




}
