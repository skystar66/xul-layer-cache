package com.xul.core.cache;

import com.xul.core.function.CacheFunctionWithParamReturn;

/**
 * 缓存接口
 *
 * @author: xl
 * @date: 2021/9/24
 **/
public interface Cache {


    /**
     * 返回缓存名称
     *
     * @param
     * @return: java.lang.String
     * @author: xl
     * @date: 2021/9/26
     **/
    String getCacheName();


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
     * 获取key的缓存值value，并将value转换成对应类型返回
     *
     * @param key
     * @param resultType
     * @return: T
     * @author: xl
     * @date: 2021/9/26
     **/
    <T> T get(String key, Class<T> resultType, CacheFunctionWithParamReturn<T,String> valueLoader);


    /**
     * 将对应的key-value缓存
     *
     * @param key
     * @param value
     * @return: void
     * @author: xl
     * @date: 2021/9/26
     **/
    void put(String key, Object value);

    /**
     * 如果缓存key没有值得时候 就进行put，如果有值的时候就返回对应的类型
     * <p>就相当于:
     * <pre><code>
     * Object existingValue = cache.get(key);
     * if (existingValue == null) {
     *     cache.put(key, value);
     *     return null;
     * } else {
     *     return existingValue;
     * }
     * </code></pre>
     *
     * @param key
     * @param value
     * @param resultType
     * @return: T
     * @author: xl
     * @date: 2021/9/26
     **/
    <T> T putIfAbsent(String key, Object value, Class<T> resultType);


    /**
     * 在缓存中移除对应的key
     *
     * @param key
     * @return: void
     * @author: xl
     * @date: 2021/9/26
     **/
    void evict(String key);


    /**
     * 清除缓存
     *
     * @param
     * @return: void
     * @author: xl
     * @date: 2021/9/26
     **/
    void clear();

    /**
     * 返回缓存元素个数
     *
     * @param
     * @return: long
     * @author: xl
     * @date: 2021/9/26
     **/
    default long estimatedSize() {
        return 0;
    }


}
