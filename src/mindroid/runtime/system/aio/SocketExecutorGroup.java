/*
 * Copyright (C) 2018 E.S.R.Labs
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

package mindroid.runtime.system.aio;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import mindroid.util.Log;

public class SocketExecutorGroup {
    private static final String LOG_TAG = "SocketExecutorGroup";
    private static final ScheduledThreadPoolExecutor sExecutor;

    private final ExecutorService mExecutorService;
    private final SocketExecutor[] mSocketExecutors;
    private final AtomicInteger mCounter = new AtomicInteger(0);

    static {
        sExecutor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("SocketExecutorGroupDaemon");
                return t;
            }
        });
        sExecutor.setKeepAliveTime(10, TimeUnit.SECONDS);
        sExecutor.allowCoreThreadTimeOut(true);
        sExecutor.setRemoveOnCancelPolicy(true);
    }

    public SocketExecutorGroup() {
        this(1);
    }

    public SocketExecutorGroup(int size) {
        this(size, null);
    }

    public SocketExecutorGroup(int size, String name) {
        if (size <= 0) {
            throw new IllegalArgumentException();
        }
        mExecutorService = Executors.newFixedThreadPool(size, new ThreadFactory() {
            private final String mName = name;
            private final AtomicInteger mCount = new AtomicInteger(1);

            public Thread newThread(Runnable r) {
                String namePrefix;
                if (mName == null) {
                    namePrefix = "";
                } else {
                    namePrefix = name + "-";
                }
                Thread t = new Thread(r, namePrefix + "SocketExecutor #" + mCount.getAndIncrement());
                t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        });

        mSocketExecutors = new SocketExecutor[size];
        for (int i = 0; i < mSocketExecutors.length; i++) {
            try {
                mSocketExecutors[i] = new SocketExecutor(mExecutorService);
            } catch (IOException e) {
                throw new RuntimeException("System failure", e);
            }
        }
    }

    public void register(SelectableSocket socket) {
        mSocketExecutors[mCounter.getAndIncrement() % mSocketExecutors.length].register(socket);
    }

    public void unregister(SelectableSocket socket) {
        for (SocketExecutor executor : mSocketExecutors) {
            executor.unregister(socket);
        }
    }

    public void shutdown() {
        for (SocketExecutor socketExecutor : mSocketExecutors) {
            socketExecutor.shutdown();
        }

        sExecutor.execute(() -> {
            try {
                mExecutorService.shutdown();
                if (!mExecutorService.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    mExecutorService.shutdownNow();
                    if (!mExecutorService.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                        Log.e(LOG_TAG, "Cannot shutdown executor service");
                    }
                }
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, "Cannot shutdown executor service", e);
                mExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        });
    }
}
