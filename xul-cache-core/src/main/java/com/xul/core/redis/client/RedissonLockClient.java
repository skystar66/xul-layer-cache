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
