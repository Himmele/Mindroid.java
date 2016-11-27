/*
 * Copyright (C) 2012 Daniel Himmelein
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

public class ThreadPoolExecutor extends Executor {
    private final int THREAD_POOL_SIZE;
    private WorkerThread[] mWorkerThreads;
    private LinkedBlockingQueue mQueue;

    public ThreadPoolExecutor(int threadPoolSize) {
        THREAD_POOL_SIZE = threadPoolSize;
        mWorkerThreads = new WorkerThread[THREAD_POOL_SIZE];
        mQueue = new LinkedBlockingQueue();
        start();
    }

    protected void finalize() {
        shutdown();
    }

    public void start() {
        for (int i = 0; i < THREAD_POOL_SIZE; i++) {
            mWorkerThreads[i] = new WorkerThread("ThreadPoolExecutor[Worker " + i + "]");
            mWorkerThreads[i].setPriority(Thread.MIN_PRIORITY);
            mWorkerThreads[i].setQueue(mQueue);
            mWorkerThreads[i].start();
        }
    }

    public void shutdown() {
        for (int i = 0; i < THREAD_POOL_SIZE; i++) {
            mWorkerThreads[i].interrupt();
            mQueue.put(null);
        }
        for (int i = 0; i < THREAD_POOL_SIZE; i++) {
            try {
                mWorkerThreads[i].join();
            } catch (InterruptedException e) {
            }
        }
    }

    public void execute(Runnable runnable) {
        mQueue.put(runnable);
    }

    public boolean cancel(Runnable runnable) {
        mQueue.remove(runnable);
        return true;
    }

    class AtomicBoolean {
        private boolean mValue;

        public AtomicBoolean(boolean value) {
            mValue = value;
        }

        public synchronized boolean get() {
            return mValue;
        }

        public synchronized void set(boolean newValue) {
            mValue = newValue;
        }

        public synchronized boolean compareAndSet(boolean expectedValue, boolean updateValue) {
            if (mValue == expectedValue) {
                mValue = updateValue;
                return true;
            }
            return false;
        }

        public synchronized boolean getAndSet(boolean newValue) {
            boolean value = mValue;
            mValue = newValue;
            return value;
        }
    }

    class WorkerThread extends Thread {
        private LinkedBlockingQueue mQueue;
        private AtomicBoolean mIsInterrupted;

        public WorkerThread(String name) {
            super(name);
            mIsInterrupted = new AtomicBoolean(false);
        }

        public void interrupt() {
            if (mIsInterrupted.compareAndSet(false, true)) {
                super.interrupt();
            }
        }

        public void run() {
            while (!mIsInterrupted.get()) {
                Runnable runnable;
                try {
                    runnable = (Runnable) mQueue.take();
                } catch (InterruptedException e) {
                    break;
                }
                runnable.run();
            }
        }

        private void setQueue(LinkedBlockingQueue queue) {
            mQueue = queue;
        }
    }
}
