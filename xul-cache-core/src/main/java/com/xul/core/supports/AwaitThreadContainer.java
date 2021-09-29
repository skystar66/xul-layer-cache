package com.xul.core.supports;


import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 等待线程容器
 *
 * @author: xl
 * @date: 2021/9/28
 **/
public class AwaitThreadContainer {


    private final Map<String, Set<Thread>> threadMap = new ConcurrentHashMap<>();

    /**
     * 线程等待，最大等待等待100ms
     *
     * @param key
     * @param milliseconds
     * @return: void
     * @author: xl
     * @date: 2021/9/28
     **/
    public final void await(String key, long milliseconds) throws InterruptedException {

        // 测试当前线程是否已经被中断
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        Set<Thread> threadSet = threadMap.get(key);
        // 判断线程容器是否是null，如果是就新创建一个
        if (threadSet == null) {
            threadSet = new ConcurrentSkipListSet<>(Comparator.comparing(Thread::toString));
            threadMap.put(key, threadSet);
        }
        // 将线程放到容器
        threadSet.add(Thread.currentThread());
        // 阻塞一定的时间
        LockSupport.parkNanos(this, TimeUnit.MILLISECONDS.toNanos(milliseconds));
    }


    /**
     * 线程唤醒
     *
     * @param key
     * @return: void
     * @author: xl
     * @date: 2021/9/28
     **/
    public final void signalAll(String key) {
        Set<Thread> threadSet = threadMap.get(key);
        // 判断key所对应的等待线程容器是否是null
        if (!CollectionUtils.isEmpty(threadSet)) {
            synchronized (threadSet) {
                if (!CollectionUtils.isEmpty(threadSet)) {
                    for (Thread thread : threadSet) {
                        LockSupport.unpark(thread);
                    }
                    // 清空等待线程容器
                    threadSet.clear();
                }
            }
        }
    }
}
