/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (C) 2013 Daniel Himmelein
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

package mindroid.os;

/**
 * Handy class for starting a new thread that has a looper. The looper can then be used to create
 * handler classes. Note that start() must still be called.
 */
public class HandlerThread extends Thread {
    private Looper mLooper;

    public HandlerThread() {
        super();
    }

    public HandlerThread(String name) {
        super(name);
    }

    public HandlerThread(ThreadGroup threadGroup, String name) {
        super(threadGroup, name);
    }

    /**
     * Call back method that can be explicitly overridden if needed to execute some setup before
     * Looper loops.
     */
    protected void onLooperPrepared() {
    }

    @Override
    public void run() {
        Looper.prepare();
        synchronized (this) {
            mLooper = Looper.myLooper();
            notifyAll();
        }
        onLooperPrepared();
        Looper.loop();
    }

    /**
     * This method returns the Looper associated with this thread. If this thread not been started
     * or for any reason is isAlive() returns false, this method will return null. If this thread
     * has been started, this method will block until the looper has been initialized.
     * 
     * @return The looper.
     */
    public Looper getLooper() {
        if (!isAlive()) {
            return null;
        }

        // If the thread has been started, wait until the looper has been created.
        synchronized (this) {
            while (isAlive() && mLooper == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    return null;
                }
            }
        }
        return mLooper;
    }

    /**
     * Ask the currently running looper to quit. If the thread has not been started or has finished
     * (that is if {@link #getLooper} returns null), then false is returned. Otherwise the looper is
     * asked to quit and true is returned.
     */
    public boolean quit() {
        Looper looper = getLooper();
        if (looper != null) {
            looper.quit();
            return true;
        }
        return false;
    }
}
