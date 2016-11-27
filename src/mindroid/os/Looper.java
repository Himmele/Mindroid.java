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

import java.lang.ThreadLocal;

/**
 * Class used to run a message loop for a thread. Threads by default do not have a message loop
 * associated with them; to create one, call {@link #prepare} in the thread that is to run the loop,
 * and then {@link #loop} to have it process messages until the loop is stopped.
 * 
 * <p>
 * Most interaction with a message loop is through the {@link Handler} class.
 * 
 * <p>
 * This is a typical example of the implementation of a Looper thread, using the separation of
 * {@link #prepare} and {@link #loop} to create an initial Handler to communicate with the Looper.
 * 
 * <pre>
 *  class LooperThread extends Thread {
 *      public Handler mHandler;
 *
 *      public void run() {
 *          Looper.prepare();
 *
 *          mHandler = new Handler() {
 *              public void handleMessage(Message msg) {
 *                  // process incoming messages here
 *              }
 *          };
 *
 *          Looper.loop();
 *      }
 *  }
 * </pre>
 */
public class Looper {
    private static final String LOG_TAG = "Looper";

    // sThreadLocal.get() will return null unless you've called prepare().
    static final ThreadLocal sThreadLocal = new ThreadLocal();
    final MessageQueue mMessageQueue;
    final Thread mThread;

    /**
     * Initialize the current thread as a looper. This gives you a chance to create handlers that
     * then reference this looper, before actually starting the loop. Be sure to call
     * {@link #loop()} after calling this method, and end it by calling {@link #quit()}.
     */
    public static void prepare() {
        prepare(true);
    }

    private static void prepare(boolean quitAllowed) {
        if (sThreadLocal.get() != null) {
            throw new RuntimeException("Only one Looper may be created per thread");
        }
        sThreadLocal.set(new Looper(quitAllowed));
    }

    /**
     * Run the message queue in this thread. Be sure to call {@link #quit()} to end the loop.
     */
    public static void loop() {
        final Looper me = myLooper();
        if (me == null) {
            throw new RuntimeException("No Looper; Looper.prepare() wasn't called on this thread");
        }
        final MessageQueue mq = me.mMessageQueue;

        for (;;) {
            Message msg = mq.dequeueMessage();
            if (msg == null) {
                // No message indicates that the message queue is quitting.
                return;
            }

            msg.target.dispatchMessage(msg);
            msg.recycle();
        }
    }

    /**
     * Return the Looper object associated with the current thread. Returns null if the calling
     * thread is not associated with a Looper.
     */
    public static Looper myLooper() {
        return (Looper) sThreadLocal.get();
    }

    /**
     * Return the {@link MessageQueue} object associated with the current thread. This must be
     * called from a thread running a Looper, or a NullPointerException will be thrown.
     */
    public static MessageQueue myQueue() {
        return myLooper().mMessageQueue;
    }

    private Looper(boolean quitAllowed) {
        mMessageQueue = new MessageQueue(quitAllowed);
        mThread = Thread.currentThread();
    }

    /**
     * Returns true if the current thread is this looper's thread.
     */
    public boolean isCurrentThread() {
        return Thread.currentThread() == mThread;
    }

    /**
     * Quits the looper.
     * 
     * Causes the {@link #loop} method to terminate as soon as possible.
     */
    public void quit() {
        mMessageQueue.quit();
    }

    /**
     * Return the Thread associated with this Looper.
     */
    public Thread getThread() {
        return mThread;
    }

    /** @hide */
    public MessageQueue getQueue() {
        return mMessageQueue;
    }

    public String toString() {
        return "Looper (" + mThread.getName() + ") {" + Integer.toHexString(System.identityHashCode(this)) + "}";
    }
}
