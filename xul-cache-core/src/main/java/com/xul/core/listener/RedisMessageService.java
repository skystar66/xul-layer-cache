package com.xul.core.listener;

import com.xul.core.cache.Cache;
import com.xul.core.cache.LayeringCache;
import com.xul.core.config.GlobalConfig;
import com.xul.core.logger.LoggerHelper;
import com.xul.core.manager.AbstractCacheManager;
import com.xul.core.utils.GSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * redis消息处理服务 <>拉去消息模式</>
 *
 * @author: xl
 * @date: 2021/9/27
 **/
@Slf4j
public class RedisMessageService {


    private static class InstanceHolder {
        public static final RedisMessageService instance = new RedisMessageService();
    }

    public static RedisMessageService getInstance() {
        return RedisMessageService.InstanceHolder.instance;
    }

    /**
     * 缓存管理器
     */
    private AbstractCacheManager cacheManager;

    /**
     * 本地消息偏移量
     */
    private static AtomicLong OFFSET = new AtomicLong(0);

    /**
     * 最后一次处理推消息的时间戳，保证可见性即可
     */
    private static volatile Long LAST_PUSH_TIME = 0l;

    /**
     * 最后一次处理拉消息的时间戳，保证可见性即可
     */
    private static volatile Long LAST_PULL_TIME = 0l;

    /**
     * pub/sub 重连时间间隔
     */
    private static Long RECONNECTION_TIME = 10 * 1000l;


    public RedisMessageService init(AbstractCacheManager cacheManager) {
        this.cacheManager = cacheManager;
        return this;

    }

    /**
     * 拉消息[startOffset,endOffset]
     *
     * @return: void
     * @author: xl
     * @date: 2021/9/29
     **/
    public void pullMessage() {
        long maxOffset = cacheManager.getRedisClient().llen(GlobalConfig.getMessageRedisKey()) - 1;
        if (maxOffset < 0) {
            return;
        }
        long oldOffset = OFFSET.getAndSet(maxOffset);
        if (oldOffset != 0 && oldOffset >= maxOffset) {
            /**本地已是最新同步的缓存信息啦*/
            return;
        }
        long endOffset = maxOffset - oldOffset - 1;
        /**获取消息*/
        List<String> messages = cacheManager.getRedisClient().lrange(GlobalConfig.getMessageRedisKey(), 0, endOffset, GlobalConfig.GLOBAL_REDIS_SERIALIZER);
        if (CollectionUtils.isEmpty(messages)) {
            return;
        }
        updateLastPullTime();
        for (String message : messages) {
            RedisPubSubMessage pubSubMessage = GSONUtil.fromJson(message, RedisPubSubMessage.class);
            if (LoggerHelper.isDebugEnabled()) {
                log.info("【缓存同步】redis 通过PULL方式处理本地缓存，startOffset:【0】,endOffset:【{}】,消息内容：{}", endOffset, message);
            }
            //获取缓存处理器
            Cache cache = cacheManager.getCacheContainer().get(pubSubMessage.getCacheName());
            if (cache != null && cache instanceof LayeringCache) {
                switch (pubSubMessage.getMessageType()) {
                    case UPDATE:
                        /**更新一级缓存*/
                        ((LayeringCache) cache).getFirstCache().put(pubSubMessage.getKey(), pubSubMessage.getValue());
                        log.info("【一级缓存同步】更新一级缓存 {} 数据,key={},消息内容={}", pubSubMessage.getCacheName(), pubSubMessage.getKey(), message);
                        break;
                    case EVICT:
                        /**清除一级缓存*/
                        ((LayeringCache) cache).getFirstCache().evict(pubSubMessage.getKey());
                        log.info("【一级缓存同步】删除一级缓存 {} 数据,key={}", pubSubMessage.getCacheName(), pubSubMessage.getKey());
                        break;
                    case CLEAR:
                        /**清理一级缓存*/
                        ((LayeringCache) cache).getFirstCache().clear();
                        log.info("【一级缓存同步】清理一级缓存 {}!", pubSubMessage.getCacheName());
                        break;
                    default:
                        log.error("接收到没有定义的消息数据");
                        break;
                }
            }
        }
    }

    /**
     * 同步offset
     *
     * @param
     * @return: void
     * @author: xl
     * @date: 2021/9/29
     **/
    public void syncOffset() {
        //同步redis 最新的offset
        long maxOffset = cacheManager.getRedisClient().llen(GlobalConfig.getMessageRedisKey()) - 1;
        if (maxOffset < 0) {
            return;
        }
        OFFSET.getAndSet(maxOffset);
        log.info("同步 OFFSET:【{}】 成功", maxOffset);
    }


    /**
     * 更新最后一次处理拉消息的时间
     *
     * @param
     * @return: void
     * @author: xl
     * @date: 2021/9/29
     **/
    public void updateLastPullTime() {
        LAST_PULL_TIME = System.currentTimeMillis();
    }


    /**
     * 更新最后一次处理推消息的时间
     *
     * @param
     * @return: void
     * @author: xl
     * @date: 2021/9/29
     **/
    public void updateLastPushTime() {
        LAST_PUSH_TIME = System.currentTimeMillis();
    }


    /**
     * 清空消息队列
     *
     * @param
     * @return: void
     * @author: xl
     * @date: 2021/9/29
     **/
    public void clearMessageQueue() {
        cacheManager.getRedisClient().tryLock(GlobalConfig.getMessageRedisKey(), 1, 15, TimeUnit.SECONDS, () -> {
            // 清空消息，直接删除key（不可以调换顺序）
            cacheManager.getRedisClient().delete(GlobalConfig.getMessageRedisKey());
        });
        // 重置偏移量，其它服务器也会更新
        OFFSET.getAndSet(-1);
    }


    /**
     * 启动重连检测，防止redis,pub ,sub 掉线
     * 相当于探活
     *
     * @param
     * @return: void
     * @author: xl
     * @date: 2021/9/29
     **/
    public void reconnection() {
        if (LAST_PUSH_TIME == 0) {
            return;
        }
        /**当前时间-上一次的推消息的时间*/
        long time = System.currentTimeMillis() - LAST_PUSH_TIME;
        if (time >= RECONNECTION_TIME) {
            try {
                updateLastPushTime();
                //  redis pub/sub 监听器
                RedisMessageListener.getInstance().init(cacheManager);
            } catch (Exception e) {
                log.error("layering-cache 清楚一级缓存异常：{}", e.getMessage(), e);
            }
        }
    }

}
