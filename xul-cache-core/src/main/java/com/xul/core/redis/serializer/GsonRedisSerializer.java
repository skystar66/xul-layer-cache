package com.xul.core.redis.serializer;

import com.xul.core.utils.GSONUtil;

/**
 * GSON序列化
 *
 * @author: xl
 * @date: 2021/9/27
 **/
public class GsonRedisSerializer implements RedisSerializer {


    @Override
    public <T> byte[] serialize(T value) throws SerializationException {
        return GSONUtil.serialize(value);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> resultType) throws SerializationException {
        return GSONUtil.deserialize(bytes, resultType);
    }
}
