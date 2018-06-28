/*
 * Copyright (C) 2018 Daniel Himmelein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mindroid.util.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import mindroid.os.Handler;
import mindroid.os.HandlerThread;

public class AsyncAwait {
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final int KEEP_ALIVE = 1;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "AsyncAwait #" + mCount.getAndIncrement());
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        }
    };

    private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<Runnable>(128);

    /**
     * An {@link Executor} that can be used to execute tasks in parallel.
     */
    public static final ExecutorService THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
            TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory);

    private static final HandlerThread sThread;

    /**
     * An {@link Executor} that executes tasks one at a time in serial
     * order. This serialization is global to a particular process.
     */
    public static final Executor SERIAL_EXECUTOR;

    static {
        sThread = new HandlerThread("AsyncAwait #0");
        sThread.start();
        SERIAL_EXECUTOR = new Handler(sThread.getLooper()).asExecutor();
    }

    /** @hide */
    public static void setUp() {
    }

    /** @hide */
    public static void tearDown() {
        sThread.quit();
        try {
            THREAD_POOL_EXECUTOR.shutdown();
            THREAD_POOL_EXECUTOR.awaitTermination(10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            THREAD_POOL_EXECUTOR.shutdownNow();
        }
    }

    public static Promise<Void> async() {
        return async(SERIAL_EXECUTOR);
    }

    public static Promise<Void> async(Handler handler) {
        return async(handler.asExecutor());
    }

    public static Promise<Void> async(Executor executor) {
        Promise<Void> promise = new Promise<>(executor);
        promise.complete(null);
        return promise;
    }

    public static void await(final Promise<?> promise) throws CancellationException, ExecutionException, InterruptedException {
        promise.get();
    }

    public static void await(final Promise<?> promise, long timeout) throws CancellationException, ExecutionException, InterruptedException, TimeoutException {
        promise.get(timeout);
    }
}
