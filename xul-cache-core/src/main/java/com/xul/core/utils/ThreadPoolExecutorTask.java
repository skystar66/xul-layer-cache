package com.xul.core.utils;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * 线程池工具类
 *
 * @author: xl
 * @date: 2021/9/28
 **/
@Slf4j
public class ThreadPoolExecutorTask {
    /**
     * IO线程池 公共
     */
    @Getter
    private final static ExecutorService taskExecutor;

    static {
        taskExecutor = new ThreadPoolExecutor(50, 100,
                1L,
                TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(10000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "layering-cache-thread");
                        thread.setDaemon(true);
                        return thread;
                    }
                },
                /*
                 * 线程池对拒绝任务(无限程可用)的处理策略
                 * ThreadPoolExecutor.AbortPolicy:丢弃任务并抛出RejectedExecutionException异常。
                 * ThreadPoolExecutor.DiscardPolicy：也是丢弃任务，但是不抛出异常。
                 * ThreadPoolExecutor.DiscardOldestPolicy：丢弃队列最前面的任务，然后重新尝试执行任务（重复此过程）
                 * ThreadPoolExecutor.CallerRunsPolicy：由调用线程处理该任务,如果执行器已关闭,则丢弃.
                 */
                new ThreadPoolExecutor.DiscardPolicy() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                        log.warn("layering-cache-thread 丢弃。");
                    }
                }
        );
    }

    public static void run(Runnable runnable) {
        taskExecutor.execute(runnable);
    }




    public static void close() {
        taskExecutor.shutdown();
    }
}
