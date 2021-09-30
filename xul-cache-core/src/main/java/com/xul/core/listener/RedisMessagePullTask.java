package com.xul.core.listener;

import com.xul.core.utils.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.Calendar;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * redis消息任务
 *
 * @author: xl
 * @date: 2021/9/29
 **/
@Slf4j
public class RedisMessagePullTask {

    private static class InstanceHolder {
        public static final RedisMessagePullTask instance = new RedisMessagePullTask();
    }

    public static RedisMessagePullTask getInstance() {
        return RedisMessagePullTask.InstanceHolder.instance;
    }

    /**
     * 定时任务线程池
     */
    private static final ScheduledThreadPoolExecutor pullTaskExecutor = new ScheduledThreadPoolExecutor(3, new NamedThreadFactory("layering-cache-pull-task-message"));

    /**
     * redis 消息处理器
     */
    RedisMessageService redisMessageService;

    /**
     * 初始化
     *
     * @param
     * @return: void
     * @author: xl
     * @date: 2021/9/29
     **/
    public void init() {
        redisMessageService = RedisMessageService.getInstance();
        /**服务启动同步最新的消息偏移量OFFSET*/
        redisMessageService.syncOffset();
        /**启动拉取消息任务线程,防止丢消息*/
        startPullTask();
        /**启动凌晨重置消息队列任务线程*/
        clearMessageQueueTask();

        /**重连检测，防止redis,pub ,sub 掉线*/
        reconnectionTask();
    }

    /**
     * 启动重连检测，防止redis,pub ,sub 掉线
     *
     * @param
     * @return: void
     * @author: xl
     * @date: 2021/9/29
     **/
    private void reconnectionTask() {
        pullTaskExecutor.scheduleWithFixedDelay(() -> {
                    redisMessageService.reconnection();
                },
                5, 5, TimeUnit.SECONDS);
        log.info("启动探活任务线程成功！！！");

    }

    /**
     * 清理消息队列，防止消息堆积，增加消息一致性，重置本地OFFSET
     *
     * @param
     * @return: void
     * @author: xl
     * @date: 2021/9/29
     **/
    private void clearMessageQueueTask() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 3);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long initialDelay = System.currentTimeMillis() - cal.getTimeInMillis();
        initialDelay = initialDelay > 0 ? initialDelay : 0;
        // 每天晚上凌晨3:00执行任务
        pullTaskExecutor.scheduleWithFixedDelay(() -> {
            try {
                redisMessageService.clearMessageQueue();
            } catch (Exception e) {
                e.printStackTrace();
                log.error("layering-cache 重置本地消息偏移量异常：{}", e.getMessage(), e);
            }
        }, initialDelay, TimeUnit.DAYS.toMillis(1), TimeUnit.MILLISECONDS);
        log.info("启动消息队列任务线程成功！！！");


    }


    /**
     * 启动拉取消息任务线程，防止丢消息【每隔30秒会检查一下本地偏移量和远程偏移量是否一致，以此来解决redis pub/sub消息丢失或者断线问题。】
     *
     * @param
     * @return: void
     * @author: xl
     * @date: 2021/9/29
     **/
    private void startPullTask() {

        pullTaskExecutor.scheduleWithFixedDelay(() -> {
            redisMessageService.pullMessage();
        }, 5, 30, TimeUnit.SECONDS);

        log.info("启动每30s 拉取消息任务线程成功！！！");
    }


}
