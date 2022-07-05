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

import mindroid.util.Log;

/**
 * Low-level class holding the list of messages to be dispatched by a {@link Looper}. Messages are
 * not added directly to a MessageQueue, but rather through {@link Handler} objects associated with
 * the Looper.
 * 
 * <p>
 * You can retrieve the MessageQueue for the current thread with {@link Looper#myQueue()
 * Looper.myQueue()}.
 */
public class MessageQueue {
    private static final String LOG_TAG = "MessageQueue";
    private static final boolean DEBUG = false;
    private static final int STARVATION_DELAY = 1000; // ms
    private final boolean mQuitAllowed;

    Message mHeadMessage;
    Message mTailMessage;
    private boolean mQuitting;

    MessageQueue(boolean quitAllowed) {
        mQuitAllowed = quitAllowed;
    }

    final void quit() {
        if (!mQuitAllowed) {
            throw new IllegalStateException("Looper thread is not allowed to quit");
        }

        synchronized (this) {
            if (mQuitting) {
                return;
            }
            mQuitting = true;

            Message curMessage = mHeadMessage;
            while (curMessage != null) {
                Message nextMessage = curMessage.nextMessage;
                curMessage.recycle();
                curMessage = nextMessage;
            }
            mHeadMessage = null;
            mTailMessage = null;

            notify();
        }
    }

    final boolean enqueueMessage(Message message, long when) {
        if (message.target == null) {
            throw new IllegalArgumentException("Message must have a target");
        }

        synchronized (this) {
            if (message.isInUse()) {
                throw new IllegalStateException(message + ": This message is already in use");
            }

            if (mQuitting) {
                IllegalStateException e = new IllegalStateException(message.target + " is sending a message to a Handler on a dead thread");
                Log.w(LOG_TAG, e.getMessage(), e);
                message.recycle();
                return false;
            }

            message.markInUse();
            message.when = when;

            if (mHeadMessage == null || when == 0 || when < mHeadMessage.when) {
                Message oldHeadMessage = mHeadMessage;
                mHeadMessage = message;
                if (oldHeadMessage != null) {
                    oldHeadMessage.prevMessage = mHeadMessage;
                } else {
                    mTailMessage = mHeadMessage;
                }
                mHeadMessage.nextMessage = oldHeadMessage;
            } else if (when >= mTailMessage.when) {
                message.prevMessage = mTailMessage;
                mTailMessage.nextMessage = message;
                mTailMessage = message;
            } else {
                Message curMessage = mTailMessage;
                Message nextMessage;
                for (;;) {
                    nextMessage = curMessage;
                    curMessage = curMessage.prevMessage;
                    if (when >= curMessage.when) {
                        break;
                    }
                }
                message.nextMessage = nextMessage;
                message.prevMessage = curMessage;
                nextMessage.prevMessage = message;
                curMessage.nextMessage = message;
            }
            notify();
        }
        return true;
    }

    final Message dequeueMessage() {
        for (;;) {
            synchronized (this) {
                if (mQuitting) {
                    return null;
                }

                final long now = SystemClock.uptimeMillis();
                Message message = mHeadMessage;

                if (message != null) {
                    if (now < message.when) {
                        try {
                            wait(Math.min(message.when - now, Integer.MAX_VALUE));
                        } catch (InterruptedException e) {
                            // Ignore wakeups.
                        }
                    } else {
                        if (DEBUG) {
                            if ((now - message.when) > STARVATION_DELAY) {
                                Log.w(LOG_TAG, "Thread '" + Thread.currentThread().getName() + "' starvation delay: " + (now - message.when) + "ms");
                            }
                        }

                        mHeadMessage = message.nextMessage;
                        if (mHeadMessage != null) {
                            mHeadMessage.prevMessage = null;
                        } else {
                            mTailMessage = null;
                        }
                        message.prevMessage = null;
                        message.nextMessage = null;
                        return message;
                    }
                } else {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        // Ignore wakeups.
                    }
                }
            }
        }
    }

    final boolean hasMessages(Handler handler, int what, Object object) {
        if (handler == null) {
            return false;
        }

        synchronized (this) {
            Message curMessage = mHeadMessage;
            while (curMessage != null) {
                if (curMessage.target == handler && curMessage.what == what && (object == null || curMessage.obj == object)) {
                    return true;
                }
                curMessage = curMessage.nextMessage;
            }
            return false;
        }
    }

    final boolean hasMessages(Handler handler, Runnable runnable, Object object) {
        if (handler == null) {
            return false;
        }

        synchronized (this) {
            Message curMessage = mHeadMessage;
            while (curMessage != null) {
                if (curMessage.target == handler && curMessage.callback == runnable && (object == null || curMessage.obj == object)) {
                    return true;
                }
                curMessage = curMessage.nextMessage;
            }
            return false;
        }
    }

    final boolean removeMessages(Handler handler, int what, Object object) {
        if (handler == null) {
            return false;
        }

        boolean foundMessage = false;

        synchronized (this) {
            Message curMessage = mHeadMessage;

            // Remove all messages at the front of the message queue.
            while (curMessage != null && curMessage.target == handler && curMessage.what == what && (object == null || curMessage.obj == object)) {
                foundMessage = true;
                Message nextMessage = curMessage.nextMessage;
                mHeadMessage = nextMessage;
                if (mHeadMessage != null) {
                    mHeadMessage.prevMessage = null;
                } else {
                    mTailMessage = null;
                }
                curMessage.recycle();
                curMessage = nextMessage;
            }

            // Remove all messages after the front of the message queue.
            while (curMessage != null) {
                Message nextMessage = curMessage.nextMessage;
                if (nextMessage != null) {
                    if (nextMessage.target == handler && nextMessage.what == what && (object == null || nextMessage.obj == object)) {
                        foundMessage = true;
                        Message nextButOneMessage = nextMessage.nextMessage;
                        if (nextButOneMessage != null) {
                            nextButOneMessage.prevMessage = curMessage;
                        } else {
                            mTailMessage = curMessage;
                        }
                        curMessage.nextMessage = nextButOneMessage;
                        nextMessage.recycle();
                        continue;
                    }
                }
                curMessage = nextMessage;
            }
        }

        return foundMessage;
    }

    final boolean removeMessages(Handler handler, Runnable runnable, Object object) {
        if (handler == null || runnable == null) {
            return false;
        }

        boolean foundMessage = false;

        synchronized (this) {
            Message curMessage = mHeadMessage;

            // Remove all messages at the front of the message queue.
            while (curMessage != null && curMessage.target == handler && curMessage.callback == runnable && (object == null || curMessage.obj == object)) {
                foundMessage = true;
                Message nextMessage = curMessage.nextMessage;
                mHeadMessage = nextMessage;
                if (mHeadMessage != null) {
                    mHeadMessage.prevMessage = null;
                } else {
                    mTailMessage = null;
                }
                curMessage.recycle();
                curMessage = nextMessage;
            }

            // Remove all messages after the front of the message queue.
            while (curMessage != null) {
                Message nextMessage = curMessage.nextMessage;
                if (nextMessage != null) {
                    if (nextMessage.target == handler && nextMessage.callback == runnable && (object == null || nextMessage.obj == object)) {
                        foundMessage = true;
                        Message nextButOneMessage = nextMessage.nextMessage;
                        if (nextButOneMessage != null) {
                            nextButOneMessage.prevMessage = curMessage;
                        } else {
                            mTailMessage = curMessage;
                        }
                        curMessage.nextMessage = nextButOneMessage;
                        nextMessage.recycle();
                        continue;
                    }
                }
                curMessage = nextMessage;
            }
        }

        return foundMessage;
    }

    final boolean removeCallbacksAndMessages(Handler handler, Object object) {
        if (handler == null) {
            return false;
        }

        boolean foundMessage = false;

        synchronized (this) {
            Message curMessage = mHeadMessage;

            // Remove all messages at the front of the message queue.
            while (curMessage != null && curMessage.target == handler && (object == null || curMessage.obj == object)) {
                foundMessage = true;
                Message nextMessage = curMessage.nextMessage;
                mHeadMessage = nextMessage;
                if (mHeadMessage != null) {
                    mHeadMessage.prevMessage = null;
                } else {
                    mTailMessage = null;
                }
                curMessage.recycle();
                curMessage = nextMessage;
            }

            // Remove all messages after the front of the message queue.
            while (curMessage != null) {
                Message nextMessage = curMessage.nextMessage;
                if (nextMessage != null) {
                    if (nextMessage.target == handler && (object == null || nextMessage.obj == object)) {
                        foundMessage = true;
                        Message nextButOneMessage = nextMessage.nextMessage;
                        if (nextButOneMessage != null) {
                            nextButOneMessage.prevMessage = curMessage;
                        } else {
                            mTailMessage = curMessage;
                        }
                        curMessage.nextMessage = nextButOneMessage;
                        nextMessage.recycle();
                        continue;
                    }
                }
                curMessage = nextMessage;
            }
        }

        return foundMessage;
    }
}
