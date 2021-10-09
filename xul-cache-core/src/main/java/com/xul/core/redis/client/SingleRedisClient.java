package com.xul.core.redis.client;

import com.xul.core.config.RedisConfig;
import com.xul.core.listener.RedisMessageListener;
import com.xul.core.redis.serializer.KryoRedisSerializer;
import com.xul.core.redis.serializer.ProtostuffRedisSerializer;
import com.xul.core.redis.serializer.RedisSerializer;
import com.xul.core.exception.RedisClientException;
import com.xul.core.function.CacheFunctionWithoutReturn;
import com.xul.core.redis.serializer.StringRedisSerializer;
import com.xul.core.utils.GSONUtil;
import com.xul.core.utils.StringUtils;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.support.ConnectionPoolSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 单节点redis处理
 *
 * @author: xl
 * @date: 2021/9/27
 **/
@Slf4j
public class SingleRedisClient implements RedisClient {


    /**
     * 默认序列化容器
     */
    private RedisSerializer keyRedisSerializer = new StringRedisSerializer();
    private RedisSerializer valueRedisSerializer = new ProtostuffRedisSerializer();

    private static GenericObjectPool<StatefulRedisConnection<byte[], byte[]>> pool;


    /**
     * redis客户端
     */
    private io.lettuce.core.RedisClient client;

    private StatefulRedisConnection<byte[], byte[]> connection;

    StatefulRedisPubSubConnection<String, String> pubSubConnection;


    /**
     * redis分布式锁
     */
    private final RedissonClient lockClient;

    public SingleRedisClient(RedisConfig redisConfig) {
        RedisURI redisURI = RedisURI.builder()
                .withHost(redisConfig.getHost())
                .withDatabase(redisConfig.getDatabase())
                .withPort(redisConfig.getPort())
                .build();
        if (StringUtils.isNotBlank(redisConfig.getPassword())) {
            redisURI.setPassword(redisConfig.getPassword());
        }

        log.info("layering-cache redis配置" + GSONUtil.toJson(redisConfig));
        this.client = io.lettuce.core.RedisClient.create(redisURI);
        this.client.setOptions(ClientOptions.builder()
                .autoReconnect(true)
                .pingBeforeActivateConnection(true)
                .build());

        GenericObjectPoolConfig genericObjectPoolConfig = new GenericObjectPoolConfig();
        //redis最大连接数
        genericObjectPoolConfig.setMaxTotal(redisConfig.getMaxSize());
        //空闲时最大连接数
        genericObjectPoolConfig.setMaxIdle(redisConfig.getIdleMaxSize());
        //空闲时最小连接数
        genericObjectPoolConfig.setMinIdle(redisConfig.getMinSize());
        pool = ConnectionPoolSupport.createGenericObjectPool(() -> {
            this.connection = client.connect(new ByteArrayCodec());
            return this.connection;
        }, genericObjectPoolConfig);
        this.pubSubConnection = client.connectPubSub();
        lockClient = new RedissonLockClient(redisConfig).getRedissonClient();
    }

    /**
     * 获取连接池
     */
    private static StatefulRedisConnection<byte[], byte[]> getLettcueRedisResource() {
        StatefulRedisConnection<byte[], byte[]> connection = null;
        try {
            connection = pool.borrowObject();     // <3> 创建线程安全的连接
        } catch (Exception ex) {
            log.error("error:{}", ex);
        }
        return connection;
    }

    /**
     * 释放连接
     *
     * @param
     * @return: void
     * @author: xl
     * @date: 2021/9/27
     **/
    private static void returnConnectionPool(StatefulRedisConnection<byte[], byte[]> connection) {
        try {
            pool.returnObject(connection);
        } catch (Exception exception) {
            /**ignore*/
        }
    }


    @Override
    public <T> T get(String key, Class<T> resultType) {
        StatefulRedisConnection<byte[], byte[]> connection = null;
        try {
            connection = getLettcueRedisResource();
            byte[] cache = connection.sync().get(keyRedisSerializer.serialize(key));
            if (cache != null) {
                return valueRedisSerializer.deserialize(cache, resultType);
            }
            return null;
        } catch (Exception exception) {
            log.error("single redis 【get】 error:{}", exception);
            throw new RedisClientException(exception.getMessage(), exception);
        } finally {
            returnConnectionPool(connection);
        }
    }


    @Override
    public <T> T get(String key, Class<T> resultType, RedisSerializer valueRedisSerializer) {
        StatefulRedisConnection<byte[], byte[]> connection = null;
        try {
            connection = getLettcueRedisResource();
            byte[] cache = connection.sync().get(keyRedisSerializer.serialize(key));
            if (cache != null) {
                return valueRedisSerializer.deserialize(cache, resultType);
            }
            return null;
        } catch (Exception exception) {
            log.error("single redis 【get】 error:{}", exception);
            throw new RedisClientException(exception.getMessage(), exception);
        } finally {
            returnConnectionPool(connection);
        }
    }

    @Override
    public void set(String key, Object value) {

        StatefulRedisConnection<byte[], byte[]> connection = null;
        try {
            connection = getLettcueRedisResource();
            connection.async().set(keyRedisSerializer.serialize(key), valueRedisSerializer.serialize(value));
        } catch (Exception exception) {
            log.error("single redis 【set】 error:{}", exception);
            throw new RedisClientException(exception.getMessage(), exception);
        } finally {
            returnConnectionPool(connection);
        }
    }

    @Override
    public void set(String key, Object value, long time, TimeUnit unit) {

        StatefulRedisConnection<byte[], byte[]> connection = null;

        try {
            connection = getLettcueRedisResource();
            connection.async().setex(keyRedisSerializer.serialize(key), unit.toSeconds(time), valueRedisSerializer.serialize(value));
        } catch (Exception exception) {
            log.error("single redis 【setExpire】 error:{}", exception);
            throw new RedisClientException(exception.getMessage(), exception);
        } finally {
            returnConnectionPool(connection);
        }
    }

    @Override
    public void set(String key, Object value, long time, TimeUnit unit, RedisSerializer valueRedisSerializer) {
        StatefulRedisConnection<byte[], byte[]> connection = null;

        try {
            connection = getLettcueRedisResource();
            connection.async().setex(keyRedisSerializer.serialize(key), unit.toSeconds(time), valueRedisSerializer.serialize(value));
        } catch (Exception exception) {
            log.error("single redis 【setExpireSerializer】 error:{}", exception);
            throw new RedisClientException(exception.getMessage(), exception);
        } finally {
            returnConnectionPool(connection);
        }
    }

    @Override
    public void setNxEx(String key, Object value, long time) {
        StatefulRedisConnection<byte[], byte[]> connection = null;

        try {
            connection = getLettcueRedisResource();
            connection.async().set(keyRedisSerializer.serialize(key),
                    valueRedisSerializer.serialize(value), SetArgs.Builder.nx().ex(time));
        } catch (Exception exception) {
            log.error("single redis 【setExpireSerializer】 error:{}", exception);
            throw new RedisClientException(exception.getMessage(), exception);

        } finally {
            returnConnectionPool(connection);
        }

    }

    @Override
    public Long delete(String... keys) {
        StatefulRedisConnection<byte[], byte[]> connection = null;
        try {
            connection = getLettcueRedisResource();
            final byte[][] bkeys = new byte[keys.length][];
            for (int i = 0; i < keys.length; i++) {
                bkeys[i] = keyRedisSerializer.serialize(keys[i]);
            }
            return connection.sync().del(bkeys);
        } catch (Exception exception) {
            log.error("single redis 【setExpireSerializer】 error:{}", exception);
            throw new RedisClientException(exception.getMessage(), exception);
        } finally {
            returnConnectionPool(connection);
        }
    }

    @Override
    public Long delete(Set<String> keys) {
        return delete(keys.toArray(new String[0]));
    }

    @Override
    public Boolean hasKey(String key) {
        StatefulRedisConnection<byte[], byte[]> connection = null;
        try {
            connection = getLettcueRedisResource();
            return connection.sync().exists(keyRedisSerializer.serialize(key)) > 0;
        } catch (Exception exception) {
            log.error("single redis 【hasKey】 error:{}", exception);
            throw new RedisClientException(exception.getMessage(), exception);
        } finally {
            returnConnectionPool(connection);
        }
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit timeUnit) {
        StatefulRedisConnection<byte[], byte[]> connection = null;
        try {
            connection = getLettcueRedisResource();
            return connection.sync().expire(keyRedisSerializer.serialize(key), timeUnit.toSeconds(timeout));
        } catch (Exception exception) {
            log.error("single redis 【expire】 error:{}", exception);
            throw new RedisClientException(exception.getMessage(), exception);
        } finally {
            returnConnectionPool(connection);
        }
    }

    @Override
    public Long getExpire(String key) {
        StatefulRedisConnection<byte[], byte[]> connection = null;

        try {
            connection = getLettcueRedisResource();
            return connection.sync().ttl(keyRedisSerializer.serialize(key));
        } catch (Exception exception) {
            log.error("single redis 【getExpire】 error:{}", exception);
            throw new RedisClientException(exception.getMessage(), exception);
        } finally {
            returnConnectionPool(connection);
        }
    }

    @Override
    public Set<String> scan(String pattern) {
        StatefulRedisConnection<byte[], byte[]> connection = null;
        HashSet<String> keys = new HashSet<>();
        try {
            connection = getLettcueRedisResource();
            boolean isFinashed;
            ScanCursor cursor = ScanCursor.INITIAL;
            do {
                KeyScanCursor<byte[]> scanCursor = connection.sync().scan(cursor,
                        ScanArgs.Builder.limit(10000).match(pattern));
                for (byte[] key : scanCursor.getKeys()) {
                    keys.add(keyRedisSerializer.deserialize(key, String.class));
                }
                isFinashed = scanCursor.isFinished();
                cursor = ScanCursor.of(scanCursor.getCursor());
            } while (!isFinashed);

        } catch (Exception exception) {
            log.error("single redis 【scan】 error:{}", exception);
            throw new RedisClientException(exception.getMessage(), exception);
        } finally {
            returnConnectionPool(connection);
        }
        return keys;
    }

    @Override
    public void lpush(String key, RedisSerializer valueRedisSerializer, String... values) {
        if (Objects.isNull(values) || values.length == 0) {
            return;
        }
        StatefulRedisConnection<byte[], byte[]> connection = null;
        try {
            connection = getLettcueRedisResource();

            final byte[][] bvalues = new byte[values.length][];
            for (int i = 0; i < values.length; i++) {
                bvalues[i] = valueRedisSerializer.serialize(values[i]);
            }
            connection.async().lpush(keyRedisSerializer.serialize(key), bvalues);
        } catch (Exception exception) {
            log.error("single redis 【lpush】 error:{}", exception);
            throw new RedisClientException(exception.getMessage(), exception);
        } finally {
            returnConnectionPool(connection);
        }
    }

    @Override
    public Long llen(String key) {
        StatefulRedisConnection<byte[], byte[]> connection = null;
        try {
            connection = getLettcueRedisResource();
            return connection.sync().llen(keyRedisSerializer.serialize(key));
        } catch (Exception exception) {
            log.error("single redis 【llen】 error:{}", exception);
            throw new RedisClientException(exception.getMessage(), exception);
        } finally {
            returnConnectionPool(connection);
        }
    }

    @Override
    public List<String> lrange(String key, long start, long end, RedisSerializer valueRedisSerializer) {
        StatefulRedisConnection<byte[], byte[]> connection = null;
        try {
            connection = getLettcueRedisResource();
            List<String> list = new ArrayList<>();
            List<byte[]> values = connection.sync().lrange(getKeySerializer().serialize(key), start, end);
            if (CollectionUtils.isEmpty(values)) {
                return list;
            }
            for (byte[] value : values) {
                list.add(valueRedisSerializer.deserialize(value, String.class));
            }
            return list;
        } catch (Exception exception) {
            log.error("single redis 【lrange】 error:{}", exception);
            throw new RedisClientException(exception.getMessage(), exception);

        } finally {
            returnConnectionPool(connection);
        }
    }

    @Override
    public Object eval(String script, List<String> keys, List<String> args) {
        StatefulRedisConnection<byte[], byte[]> connection = null;

        try {
            connection = getLettcueRedisResource();
            List<byte[]> bkeys = keys.stream().map(key -> getKeySerializer().serialize(key)).collect(Collectors.toList());
            List<byte[]> bargs = args.stream().map(arg -> getValueSerializer().serialize(arg)).collect(Collectors.toList());
            return connection.sync().eval(script, ScriptOutputType.INTEGER, bkeys.toArray(new byte[0][0]), bargs.toArray(new byte[0][0]));
        } catch (Exception exception) {
            log.error("single redis 【eval】 error:{}", exception);
            throw new RedisClientException(exception.getMessage(), exception);

        } finally {
            returnConnectionPool(connection);
        }
    }

    @Override
    public void publish(String channel, String message) {
        try {

            pubSubConnection.async().publish(channel, message);
        } catch (Exception exception) {
            log.error("single redis 【publish】 error:{}", exception);
            throw new RedisClientException(exception.getMessage(), exception);

        }
    }

    @Override
    public void subscribe(RedisMessageListener messageListener, String... channel) {
        try {
            StatefulRedisPubSubConnection<String, String> pubSubConnection = client.connectPubSub();

            pubSubConnection.async().subscribe(channel);
            pubSubConnection.addListener(messageListener);
        } catch (Exception exception) {
            log.error("single redis 【subscribe】 error:{}", exception);
            throw new RedisClientException(exception.getMessage(), exception);
        }
    }

    @Override
    public boolean tryLock(String key, long waitTime, long expireTime, TimeUnit timeUnit, CacheFunctionWithoutReturn bussiness) {
        RLock lock = lockClient.getLock(key);
        try {
            if (lock.tryLock(waitTime, expireTime, timeUnit)) {
                bussiness.invokeMethod();
                return true;
            }
            return false;
        } catch (Exception exception) {
            log.error("single redis 【tryLock】 error:{}", exception);
            throw new RedisClientException(exception.getMessage(), exception);
        } finally {
            lock.unlock();
        }
    }


    @Override
    public RedisSerializer getKeySerializer() {
        return keyRedisSerializer;
    }

    @Override
    public RedisSerializer getValueSerializer() {
        return valueRedisSerializer;
    }

    @Override
    public void setKeySerializer(RedisSerializer keySerializer) {
        this.keyRedisSerializer = keySerializer;
    }

    @Override
    public void setValueSerializer(RedisSerializer valueSerializer) {
        this.valueRedisSerializer = valueSerializer;
    }
}
