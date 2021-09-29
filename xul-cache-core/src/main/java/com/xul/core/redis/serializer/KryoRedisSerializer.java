package com.xul.core.redis.serializer;

import com.xul.core.supports.KryoSerializer;

/**
 * KRYO序列化
 *
 * @author: xl
 * @date: 2021/9/27
 **/
public class KryoRedisSerializer implements RedisSerializer {


    @Override
    public <T> byte[] serialize(T value) throws SerializationException {
        return KryoSerializer.writeObjectToByteArray(value);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> resultType) throws SerializationException {
        return KryoSerializer.readFromByteArray(bytes, resultType);
    }
}
