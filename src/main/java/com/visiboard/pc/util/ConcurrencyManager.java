package com.visiboard.pc.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrencyManager {
    private static final int THREAD_POOL_SIZE = 4;
    
    private static final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE, new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "VisiBoard-Worker-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    });

    public static ExecutorService getExecutor() {
        return executorService;
    }
    
    public static void shutdown() {
        executorService.shutdown();
    }
}
