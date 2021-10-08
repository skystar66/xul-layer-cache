package com.xul.cache.starter.config;


import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 自动初始化分布式缓存
 * <p>
 * 实现分布式缓存的服务需要注册此接口
 * </p>
 *
 * @author: xl
 * @date: 2021/9/29
 **/
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({LayeringCacheAutoConfig.class})
public @interface EnableLayeringCache {
}
