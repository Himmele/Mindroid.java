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
 * Low-level class holding the list of messages to be dispatched by a
 * {@link Looper}.  Messages are not added directly to a MessageQueue,
 * but rather through {@link Handler} objects associated with the Looper.
 * 
 * <p>You can retrieve the MessageQueue for the current thread with
 * {@link Looper#myQueue() Looper.myQueue()}.
 */
public class MessageQueue {
    private static final String LOG_TAG = "MessageQueue";
    private final boolean mAllowQuitMessage;

    Message mMessages;
    private boolean mQuiting;

    MessageQueue(boolean allowQuitMessage) {
        mAllowQuitMessage = allowQuitMessage;
    }

    final void quit() {
        if (!mAllowQuitMessage) {
            throw new RuntimeException("Looper thread is not allowed to quit.");
        }

        synchronized (this) {
            if (mQuiting) {
                return;
            }
            mQuiting = true;
            notify();
        }
    }

    final boolean enqueueMessage(Message message, long when) {
        if (message.when != 0) {
            throw new RuntimeException(message + ": This message is already in use.");
        }
        if (message.target == null) {
            throw new RuntimeException("Message must have a target.");
        }

        synchronized (this) {
            if (mQuiting) {
                if (!(message.target instanceof Binder)) {
                    RuntimeException e = new RuntimeException(message.target + " is sending a message to a Handler on a dead thread");
                    Log.w(LOG_TAG, e.getMessage(), e);
                    return false;
                } else {
                    RemoteException e = new RemoteException(message.target + " is sending a message to a dead Binder");
                    Log.w(LOG_TAG, e.getMessage(), e);
                    throw e;
                }
            }

            message.when = when;
            Message curMessage = mMessages;
            if (curMessage == null || when == 0 || when < curMessage.when) {
                message.nextMessage = curMessage;
                mMessages = message;
                notify();
            } else {
                Message prevMessage;
                for (;;) {
                    prevMessage = curMessage;
                    curMessage = curMessage.nextMessage;
                    if (curMessage == null || when < curMessage.when) {
                        break;
                    }
                }
                message.nextMessage = curMessage;
                prevMessage.nextMessage = message;
                notify();
            }
        }
        return true;
    }
    
    final Message dequeueMessage() {
        for (;;) {
            synchronized (this) {
                if (mQuiting) {
                    return null;
                }

                final long now = SystemClock.uptimeMillis();                
                Message message = mMessages;

                if (message != null) {
                    if (now < message.when) {
                        try {
							wait(Math.min(message.when - now, Integer.MAX_VALUE));
						} catch (InterruptedException e) {
							// Ignore wakeups
						}
                    } else {
                        mMessages = message.nextMessage;
                        message.nextMessage = null;
                        return message;
                    }
                } else {
                    try {
						wait();
					} catch (InterruptedException e) {
						// Ignore wakeups
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
            Message curMessage = mMessages;
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
            Message curMessage = mMessages;
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
            Message curMessage = mMessages;

            // Remove all messages at the front of the message queue.
            while (curMessage != null && curMessage.target == handler && curMessage.what == what
                   && (object == null || curMessage.obj == object)) {
            	foundMessage = true;
                Message nextMessage = curMessage.nextMessage;
                mMessages = nextMessage;
                curMessage.recycle();
                curMessage = nextMessage;
            }

            // Remove all messages after the front of the message queue.
            while (curMessage != null) {
                Message nextMessage = curMessage.nextMessage;
                if (nextMessage != null) {
                    if (nextMessage.target == handler && nextMessage.what == what
                        && (object == null || nextMessage.obj == object)) {
                    	foundMessage = true;
                        Message nextButOneMessage = nextMessage.nextMessage;
                        nextMessage.recycle();
                        curMessage.nextMessage = nextButOneMessage;
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
            Message curMessage = mMessages;

            // Remove all messages at the front of the message queue.
            while (curMessage != null && curMessage.target == handler && curMessage.callback == runnable
                   && (object == null || curMessage.obj == object)) {
            	foundMessage = true;
                Message nextMessage = curMessage.nextMessage;
                mMessages = nextMessage;
                curMessage.recycle();
                curMessage = nextMessage;
            }

            // Remove all messages after the front of the message queue.
            while (curMessage != null) {
                Message nextMessage = curMessage.nextMessage;
                if (nextMessage != null) {
                    if (nextMessage.target == handler && nextMessage.callback == runnable
                        && (object == null || nextMessage.obj == object)) {
                    	foundMessage = true;
                        Message nextButOneMessage = nextMessage.nextMessage;
                        nextMessage.recycle();
                        curMessage.nextMessage = nextButOneMessage;
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
            Message curMessage = mMessages;

            // Remove all messages at the front of the message queue.
            while (curMessage != null && curMessage.target == handler
                    && (object == null || curMessage.obj == object)) {
            	foundMessage = true;
                Message nextMessage = curMessage.nextMessage;
                mMessages = nextMessage;
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
                        nextMessage.recycle();
                        curMessage.nextMessage = nextButOneMessage;
                        continue;
                    }
                }
                curMessage = nextMessage;
            }
        }
        
        return foundMessage;
    }
}
