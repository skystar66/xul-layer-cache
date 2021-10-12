package com.xul.core.redis.client;

import com.xul.core.config.RedisConfig;
import com.xul.core.utils.StringUtils;
import lombok.Getter;
import org.redisson.Redisson;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;

import java.util.ArrayList;
import java.util.List;

public class RedissonLockClient {


    @Getter
    private org.redisson.api.RedissonClient redissonClient;


    /**
     * 获取数据库key上锁的前缀
     */
    public static final String EXECUTE_DB_LOCK_PREFIX = "layering-cache:execute-db:%s";

    /**
     * redis续期分布式锁前缀
     */
    public static final String TERM_REDIS_LOCK_PREFIX = "layering-cache:term-redis:%s";

    /**
     * 获取执行数据库初始化数据的分布式锁
     *
     * @param key
     * @return: java.lang.String
     * @author: xl
     * @date: 2021/10/12
     **/
    public static String getExecuteDbLockKey(String key) {
        return String.format(EXECUTE_DB_LOCK_PREFIX, key);
    }

    /**
     * 获取续期redis的分布式锁
     *
     * @param key
     * @return: java.lang.String
     * @author: xl
     * @date: 2021/10/12
     **/
    public static String getTermRedisLockPrefix(String key) {
        return String.format(TERM_REDIS_LOCK_PREFIX, key);

    }


    public RedissonLockClient(RedisConfig redisConfig) {
        Config config = new Config();
        if (StringUtils.isNotBlank(redisConfig.getCluster())) {
            /**集群版本*/
            String cluster = redisConfig.getCluster();
            String[] parts = cluster.split("\\,");
            List<String> clusterNodes = new ArrayList<>();
            for (String address : parts) {
                clusterNodes.add("redis://" + address);
            }
            // 添加集群地址
            ClusterServersConfig clusterServersConfig = config.useClusterServers().addNodeAddress(clusterNodes.toArray(new String[clusterNodes.size()]));
            // 设置密码
            clusterServersConfig.setPassword(redisConfig.getPassword());
        } else {
            /**单机版本*/
            config.useSingleServer().setAddress("redis://" + redisConfig.getHost() + ":" + redisConfig.getPort()).setPassword(redisConfig.getPassword());
            //添加主从配置
            //config.useMasterSlaveServers().setMasterAddress("").setPassword("").addSlaveAddress(new String[]{"",""});
        }
        redissonClient = Redisson.create(config);
    }

}
