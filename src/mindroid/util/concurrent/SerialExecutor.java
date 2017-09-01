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

import mindroid.os.Handler;
import mindroid.os.HandlerThread;
import mindroid.util.Log;

public class SerialExecutor extends Executor {
    private static final String LOG_TAG = "SerialExecutor";

    private final String mName;
    private final boolean mShutdownAllowed;
    private HandlerThread mHandlerThread;
    private Handler mHandler;

    public SerialExecutor(String name) {
        this(name, true);
    }

    public SerialExecutor(String name, boolean shutdownAllowed) {
        mName = name;
        mShutdownAllowed = shutdownAllowed;
        start();
    }

    protected void finalize() {
        shutdown(10000, true);
    }

    private void start() {
        mHandlerThread = new HandlerThread((mName != null ? mName : "SerialExecutor") + "[Worker]");
        mHandlerThread.setPriority(Thread.MIN_PRIORITY);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
    }

    public boolean shutdown(long timeout) {
        return shutdown(timeout, mShutdownAllowed);
    }

    private boolean shutdown(long timeout, boolean shutdownAllowed) {
        if (!shutdownAllowed) {
            IllegalStateException e = new IllegalStateException("Worker thread is not allowed to shut down");
            Log.w(LOG_TAG, e.getMessage(), e);
            return false;
        }
        if (mHandlerThread != null) {
            mHandlerThread.getLooper().quit();
            try {
                mHandlerThread.join((timeout <= 0) ? 1 : timeout);
                if (mHandlerThread.isAlive()) {
                    Log.e(LOG_TAG, "Cannot join thread " + mHandlerThread.getName());
                }
            } catch (InterruptedException ignore) {
            }
            mHandlerThread = null;
        }
        return true;
    }

    public void execute(Runnable runnable) {
        mHandler.post(runnable);
    }

    public boolean cancel(Runnable runnable) {
        return mHandler.removeCallbacks(runnable);
    }
}
