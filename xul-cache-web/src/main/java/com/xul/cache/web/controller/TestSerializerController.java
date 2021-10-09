package com.xul.cache.web.controller;


import com.sun.management.OperatingSystemMXBean;
import com.xul.cache.web.entity.User;
import com.xul.core.manager.LayeringCacheManager;
import com.xul.core.redis.client.RedisClient;
import com.xul.core.redis.serializer.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/serializer/")
@Slf4j
public class TestSerializerController {


    @RequestMapping("test")
    public String put() {

        testSerializer();
        return "success";
    }


    /**
     * testSerializer
     * <p>
     * KryoRedisSerializer:269 b
     * fastJsonRedisSerializer:365 b
     * jacksonRedisSerializer:502 b
     * jdkRedisSerializer:1015 b
     * protostuffRedisSerializer:268 b
     * <p>
     * KryoRedisSerializer serialize:3088 ms
     * fastJsonRedisSerializer serialize:3189 ms
     * jacksonRedisSerializer serialize:2229 ms
     * jdkRedisSerializer serialize:9453 ms
     * protostuffRedisSerializer serialize:1466 ms
     * <p>
     * KryoRedisSerializer deserialize:3345 ms
     * fastJsonRedisSerializer deserialize:14493 ms
     * jacksonRedisSerializer deserialize:5231 ms
     * jdkRedisSerializer deserialize:33244 ms
     * protostuffRedisSerializer deserialize:2076 ms
     *
     * @param
     * @return: void
     * @author: xl
     * @date: 2021/10/9
     **/
    public static void testSerializer() {
        User user = new User();
        KryoRedisSerializer kryoRedisSerializer = new KryoRedisSerializer();
        FastJsonRedisSerializer fastJsonRedisSerializer = new FastJsonRedisSerializer();
        JacksonRedisSerializer jacksonRedisSerializer = new JacksonRedisSerializer();
        JdkRedisSerializer jdkRedisSerializer = new JdkRedisSerializer();
        ProtostuffRedisSerializer protostuffRedisSerializer = new ProtostuffRedisSerializer();

        byte[] kryoUserBytes = kryoRedisSerializer.serialize(user);
        byte[] fastjsonUserBytes = fastJsonRedisSerializer.serialize(user);
        byte[] jackjsonUserBytes = jacksonRedisSerializer.serialize(user);
        byte[] jdkUserBytes = jdkRedisSerializer.serialize(user);
        byte[] protostufUserBytes = protostuffRedisSerializer.serialize(user);

        int count = 500_000;
        long start = System.currentTimeMillis();

        /**kryo 序列化*/
        for (int i = 0; i < count; i++) {
            kryoRedisSerializer.serialize(user);
        }
        long kryoSet = System.currentTimeMillis() - start;

        /**fastjson 序列化*/
        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            fastJsonRedisSerializer.serialize(user);
        }
        long fastJsonSet = System.currentTimeMillis() - start;

        /**jackjson 序列化*/
        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            jacksonRedisSerializer.serialize(user);
        }
        long jacksonSet = System.currentTimeMillis() - start;

        /**jdk 序列化*/
        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            jdkRedisSerializer.serialize(user);
        }
        long jdkSet = System.currentTimeMillis() - start;
        /**protostuff 序列化*/
        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            protostuffRedisSerializer.serialize(user);
        }
        long protostufSet = System.currentTimeMillis() - start;

        /**kryo 反序列化*/

        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            kryoRedisSerializer.deserialize(kryoUserBytes, User.class);
        }
        long kryoGet = System.currentTimeMillis() - start;

        /**fastjson 反序列化*/

        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {

            fastJsonRedisSerializer.deserialize(fastjsonUserBytes, User.class);

        }
        long fastJsonGet = System.currentTimeMillis() - start;
        /**jackjson 反序列化*/

        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            jacksonRedisSerializer.deserialize(jackjsonUserBytes, User.class);
        }
        long jacksonGet = System.currentTimeMillis() - start;

        /**jdk 反序列化*/

        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            jdkRedisSerializer.deserialize(jdkUserBytes, User.class);
        }
        long jdkGet = System.currentTimeMillis() - start;

        /**protostuffer 反序列化*/

        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            protostuffRedisSerializer.deserialize(protostufUserBytes, User.class);

        }
        long protostufGet = System.currentTimeMillis() - start;

        /** 各个序列化工具 耗时打印*/

        System.out.println("KryoRedisSerializer:" + kryoRedisSerializer.serialize(user).length + " b");
        System.out.println("fastJsonRedisSerializer:" + fastJsonRedisSerializer.serialize(user).length + " b");
        System.out.println("jacksonRedisSerializer:" + jacksonRedisSerializer.serialize(user).length + " b");
        System.out.println("jdkRedisSerializer:" + jdkRedisSerializer.serialize(user).length + " b");
        System.out.println("protostuffRedisSerializer:" + protostuffRedisSerializer.serialize(user).length + " b");
        System.out.println();
        /** 各个反序列化工具 耗时打印*/

        System.out.println("KryoRedisSerializer serialize:" + kryoSet + " ms  ");
        System.out.println("fastJsonRedisSerializer serialize:" + fastJsonSet + " ms  ");
        System.out.println("jacksonRedisSerializer serialize:" + jacksonSet + " ms  ");
        System.out.println("jdkRedisSerializer serialize:" + jdkSet + " ms  ");
        System.out.println("protostuffRedisSerializer serialize:" + protostufSet + " ms  ");
        System.out.println();

        System.out.println("KryoRedisSerializer deserialize:" + kryoGet + " ms  ");
        System.out.println("fastJsonRedisSerializer deserialize:" + fastJsonGet + " ms  ");
        System.out.println("jacksonRedisSerializer deserialize:" + jacksonGet + " ms  ");
        System.out.println("jdkRedisSerializer deserialize:" + jdkGet + " ms  ");
        System.out.println("protostuffRedisSerializer deserialize:" + protostufGet + " ms  ");


        System.out.println(systemInfo());
    }


    public void testRedisSerializer() {
        User user = new User();
        KryoRedisSerializer kryoRedisSerializer = new KryoRedisSerializer();
        FastJsonRedisSerializer fastJsonRedisSerializer = new FastJsonRedisSerializer();
        JacksonRedisSerializer jacksonRedisSerializer = new JacksonRedisSerializer();
        JdkRedisSerializer jdkRedisSerializer = new JdkRedisSerializer();
        ProtostuffRedisSerializer protostuffRedisSerializer = new ProtostuffRedisSerializer();

        RedisClient redisClient = LayeringCacheManager.getInstance().getRedisClient();

        int count = 100_000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            redisClient.set("Serializer:KryoRedisSerializer", user, 10, TimeUnit.MINUTES, kryoRedisSerializer);
        }
        long kryoSet = System.currentTimeMillis() - start;
        String kryoSetSInfo = systemInfo();

        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            redisClient.set("Serializer:fastJsonRedisSerializer", user, 10, TimeUnit.MINUTES, fastJsonRedisSerializer);
        }
        long fastJsonSet = System.currentTimeMillis() - start;
        String fastJsonSetSInfo = systemInfo();

        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            redisClient.set("Serializer:jacksonRedisSerializer", user, 10, TimeUnit.MINUTES, jacksonRedisSerializer);
        }
        long jacksonSet = System.currentTimeMillis() - start;
        String jacksonSetSInfo = systemInfo();

        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            redisClient.set("Serializer:jdkRedisSerializer", user, 10, TimeUnit.MINUTES, jdkRedisSerializer);
        }
        long jdkSet = System.currentTimeMillis() - start;
        String jdkSetInfo = systemInfo();

        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            redisClient.set("Serializer:protostuffRedisSerializer", user, 10, TimeUnit.MINUTES, protostuffRedisSerializer);
        }
        long protostufSet = System.currentTimeMillis() - start;
        String protostufSetInfo = systemInfo();

        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            redisClient.get("Serializer:KryoRedisSerializer", User.class, kryoRedisSerializer);
        }
        long kryoGet = System.currentTimeMillis() - start;
        String kryoGetInfo = systemInfo();

        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            redisClient.get("Serializer:fastJsonRedisSerializer", User.class, fastJsonRedisSerializer);
        }
        long fastJsonGet = System.currentTimeMillis() - start;
        String fastJsonGetInfo = systemInfo();

        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            redisClient.get("Serializer:jacksonRedisSerializer", User.class, jacksonRedisSerializer);
        }
        long jacksonGet = System.currentTimeMillis() - start;
        String jacksonGetInfo = systemInfo();

        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            redisClient.get("Serializer:jdkRedisSerializer", User.class, jdkRedisSerializer);
        }
        long jdkGet = System.currentTimeMillis() - start;
        String jdkGetInfo = systemInfo();

        start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            redisClient.get("Serializer:protostuffRedisSerializer", User.class, protostuffRedisSerializer);
        }
        long protostufGet = System.currentTimeMillis() - start;
        String protostufGetInfo = systemInfo();


        System.out.println("KryoRedisSerializer:" + kryoRedisSerializer.serialize(user).length + " b");
        System.out.println("fastJsonRedisSerializer:" + fastJsonRedisSerializer.serialize(user).length + " b");
        System.out.println("jacksonRedisSerializer:" + jacksonRedisSerializer.serialize(user).length + " b");
        System.out.println("jdkRedisSerializer:" + jdkRedisSerializer.serialize(user).length + " b");
        System.out.println("protostuffRedisSerializer:" + protostuffRedisSerializer.serialize(user).length + " b");
        System.out.println();

        System.out.println("KryoRedisSerializer serialize:" + kryoSet + " ms  " + kryoSetSInfo);
        System.out.println("fastJsonRedisSerializer serialize:" + fastJsonSet + " ms  " + fastJsonSetSInfo);
        System.out.println("jacksonRedisSerializer serialize:" + jacksonSet + " ms  " + jacksonSetSInfo);
        System.out.println("jdkRedisSerializer serialize:" + jdkSet + " ms  " + jdkSetInfo);
        System.out.println("protostuffRedisSerializer serialize:" + protostufSet + " ms  " + protostufSetInfo);
        System.out.println();

        System.out.println("KryoRedisSerializer deserialize:" + kryoGet + " ms  " + kryoGetInfo);
        System.out.println("fastJsonRedisSerializer deserialize:" + fastJsonGet + " ms  " + fastJsonGetInfo);
        System.out.println("jacksonRedisSerializer deserialize:" + jacksonGet + " ms  " + jacksonGetInfo);
        System.out.println("jdkRedisSerializer deserialize:" + jdkGet + " ms  " + jdkGetInfo);
        System.out.println("protostuffRedisSerializer deserialize:" + protostufGet + " ms  " + protostufGetInfo);


        System.out.println(systemInfo());
    }

    private static String systemInfo() {
        OperatingSystemMXBean osmxb = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        //获取CPU
        double cpuLoad = osmxb.getSystemCpuLoad();
        int percentCpuLoad = (int) (cpuLoad * 100);

        //获取内存
        double totalvirtualMemory = osmxb.getTotalPhysicalMemorySize();
        double freePhysicalMemorySize = osmxb.getFreePhysicalMemorySize();

        double value = freePhysicalMemorySize / totalvirtualMemory;
        int percentMemoryLoad = (int) ((1 - value) * 100);

        return String.format("CPU = %s,Mem = %s", percentCpuLoad, percentMemoryLoad);
    }


}
