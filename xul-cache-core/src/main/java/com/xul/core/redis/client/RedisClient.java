package com.xul.core.redis.client;

import com.xul.core.config.RedisConfig;
import com.xul.core.listener.RedisMessageListener;
import com.xul.core.redis.serializer.RedisSerializer;
import com.xul.core.function.CacheFunctionWithoutReturn;
import com.xul.core.utils.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * redis客户端
 *
 * @author: xl
 * @date: 2021/9/27
 **/
public interface RedisClient {

    /**
     * 获取RedisClient实例
     *
     * @param redisConfig redis配置
     * @return RedisClient
     */
    static RedisClient getInstance(RedisConfig redisConfig) {
        RedisClient redisClient;
        if (StringUtils.isNotBlank(redisConfig.getCluster())) {
            redisClient = new ClusterRedisClient(redisConfig);
        } else {
            redisClient = new SingleRedisClient(redisConfig);
        }
        return redisClient;
    }


    /**
     * 获取key的缓存值value，并将value转换成对应类型返回
     *
     * @param key
     * @param resultType
     * @return: T
     * @author: xl
     * @date: 2021/9/26
     **/
    <T> T get(String key, Class<T> resultType);


    /**
     * 获取key的缓存值value，并将value转换成对应序列化类型返回
     *
     * @param key
     * @param resultType
     * @param valueRedisSerializer
     * @return: T
     * @author: xl
     * @date: 2021/10/9
     **/
    <T> T get(String key, Class<T> resultType, RedisSerializer valueRedisSerializer);


    /**
     * <p>
     * 向redis存入key和value,并释放连接资源
     * </p>
     * <p>
     * 如果key已经存在 则覆盖
     * </p>
     *
     * @param key   key
     * @param value value
     * @return 成功 返回OK 失败返回 0
     */
    void set(String key, Object value);

    /**
     * <p>
     * 向redis存入key和value,并释放连接资源
     * </p>
     * <p>
     * 如果key已经存在 则覆盖
     * </p>
     *
     * @param key   key
     * @param value value
     * @param time  时间
     * @param unit  时间单位
     * @return 成功 返回OK 失败返回 0
     */
    void set(String key, Object value, long time, TimeUnit unit);


    /**
     * <p>
     * 向redis存入key和value,并释放连接资源
     * </p>
     * <p>
     * 如果key已经存在 则覆盖
     * </p>
     *
     * @param key                  key
     * @param value                value
     * @param time                 时间
     * @param unit                 时间单位
     * @param valueRedisSerializer 指定序列化器
     * @return 成功 返回OK 失败返回 0
     */
    void set(String key, Object value, long time, TimeUnit unit, RedisSerializer valueRedisSerializer);


    /**
     * Set the string value as value of the key. The string can't be longer than 1073741824 bytes (1
     * GB).
     *
     * @param key   key
     * @param value value
     * @param time  expire time in the units of <code>expx</code>
     * @return Status code reply
     */
    void setNxEx(final String key, final Object value, final long time);

    /**
     * <p>
     * 删除指定的key,也可以传入一个包含key的数组
     * </p>
     *
     * @param keys 一个key 也可以使 string 数组
     * @return 返回删除成功的个数
     */
    Long delete(String... keys);

    /**
     * <p>
     * 删除一批key
     * </p>
     *
     * @param keys key的Set集合
     * @return 返回删除成功的个数
     */
    Long delete(Set<String> keys);


    /**
     * <p>
     * 判断key是否存在
     * </p>
     *
     * @param key key
     * @return true OR false
     */
    Boolean hasKey(String key);

    /**
     * <p>
     * 为给定 key 设置生存时间，当 key 过期时(生存时间为 0 )，它会被自动删除。
     * </p>
     *
     * @param key      key
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     * @return 成功返回1 如果存在 和 发生异常 返回 0
     */
    Boolean expire(String key, long timeout, TimeUnit timeUnit);


    /**
     * <p>
     * 以秒为单位，返回给定 key 的剩余生存时间
     * </p>
     *
     * @param key key
     * @return 当 key 不存在时或没有设置剩余生存时间时，返回 -1 。否则，以秒为单位，返回 key
     * 的剩余生存时间。 发生异常 返回 0
     */
    Long getExpire(String key);

    /**
     * <p>
     * 查询符合条件的key
     * </p>
     *
     * @param pattern 表达式
     * @return 返回符合条件的key
     */
    Set<String> scan(String pattern);

    /**
     * <p>
     * 通过key向list头部添加字符串
     * </p>
     *
     * @param key                  key
     * @param valueRedisSerializer 指定序列化器
     * @param values               可以使一个string 也可以使string数组
     * @return 返回list的value个数
     */
    void lpush(String key, RedisSerializer valueRedisSerializer, String... values);

    /**
     * <p>
     * 通过key返回list的长度
     * </p>
     *
     * @param key key
     * @return long
     */
    Long llen(String key);

    /**
     * <p>
     * 通过key获取list指定下标位置的value
     * </p>
     * <p>
     * 如果start 为 0 end 为 -1 则返回全部的list中的value
     * </p>
     *
     * @param key                  key
     * @param start                起始位置
     * @param end                  结束位置
     * @param valueRedisSerializer 指定序列化器
     * @return List
     */
    List<String> lrange(String key, long start, long end, RedisSerializer valueRedisSerializer);

    /**
     * 执行Lua脚本
     *
     * @param script Lua 脚本
     * @param keys   参数
     * @param args   参数值
     * @return 返回结果
     */
    Object eval(String script, List<String> keys, List<String> args);

    /**
     * 发送消息
     *
     * @param channel 发送消息的频道
     * @param message 消息内容
     * @return Long
     */
    void publish(String channel, String message);

    /**
     * 绑定监听器
     *
     * @param messageListener 消息监听器
     * @param channel         信道
     */
    void subscribe(RedisMessageListener messageListener, String... channel);


    /**
     * 获取分布式锁并释放锁并返回业务执行结果
     *
     * @param key
     * @param waitTime   获取锁的等待时间
     * @param expireTime 获取锁后的过期时间
     * @param timeUnit   时间单位
     * @param bussiness  执行业务函数
     * @return: T
     * @author: xl
     * @date: 2021/9/28
     **/
    public boolean tryLock(String key, long waitTime, long expireTime, TimeUnit timeUnit, CacheFunctionWithoutReturn bussiness);

    /**
     * key序列化方式
     *
     * @return the key {@link RedisSerializer}.
     */
    RedisSerializer getKeySerializer();

    /**
     * value序列化方式
     *
     * @return the value {@link RedisSerializer}.
     */
    RedisSerializer getValueSerializer();

    /**
     * 设置key的序列化方式
     *
     * @param keySerializer {@link RedisSerializer}
     */
    void setKeySerializer(RedisSerializer keySerializer);

    /**
     * 设置value的序列化方式
     *
     * @param valueSerializer {@link RedisSerializer}
     */
    void setValueSerializer(RedisSerializer valueSerializer);


}
