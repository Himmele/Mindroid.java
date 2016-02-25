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
 * A Handler allows you to send and process {@link Message} and Runnable objects associated with a
 * thread's {@link MessageQueue}. Each Handler instance is associated with a single thread and that
 * thread's message queue. When you create a new Handler, it is bound to the thread / message queue
 * of the thread that is creating it -- from that point on, it will deliver messages and runnables
 * to that message queue and execute them as they come out of the message queue.
 * 
 * <p>
 * There are two main uses for a Handler: (1) to schedule messages and runnables to be executed as
 * some point in the future; and (2) to enqueue an action to be performed on a different thread than
 * your own.
 * 
 * <p>
 * Scheduling messages is accomplished with the {@link #post}, {@link #postAtTime(Runnable, long)},
 * {@link #postDelayed}, {@link #sendEmptyMessage}, {@link #sendMessage}, {@link #sendMessageAtTime}
 * , and {@link #sendMessageDelayed} methods. The <em>post</em> versions allow you to enqueue
 * Runnable objects to be called by the message queue when they are received; the
 * <em>sendMessage</em> versions allow you to enqueue a {@link Message} object containing a bundle
 * of data that will be processed by the Handler's {@link #handleMessage} method (requiring that you
 * implement a subclass of Handler).
 * 
 * <p>
 * When posting or sending to a Handler, you can either allow the item to be processed as soon as
 * the message queue is ready to do so, or specify a delay before it gets processed or absolute time
 * for it to be processed. The latter two allow you to implement timeouts, ticks, and other
 * timing-based behavior.
 * 
 * <p>
 * When a process is created for your application, its main thread is dedicated to running a message
 * queue that takes care of managing the top-level application objects (activities, broadcast
 * receivers, etc) and any windows they create. You can create your own threads, and communicate
 * back with the main application thread through a Handler. This is done by calling the same
 * <em>post</em> or <em>sendMessage</em> methods as before, but from your new thread. The given
 * Runnable or Message will then be scheduled in the Handler's message queue and processed when
 * appropriate.
 */
public class Handler {
	/*
	 * Set this flag to true to detect anonymous, local or member classes that extend this Handler
	 * class and that are not static. These kind of classes can potentially create leaks.
	 */
	private static final String LOG_TAG = "Handler";

	final MessageQueue mMessageQueue;
	final Looper mLooper;
	final Callback mCallback;

	/**
	 * Callback interface you can use when instantiating a Handler to avoid having to implement your
	 * own subclass of Handler.
	 */
	public interface Callback {
		public boolean handleMessage(Message msg);
	}

	/**
	 * Subclasses must implement this to receive messages.
	 */
	public void handleMessage(Message msg) {
	}

	/**
	 * Handle system messages here.
	 */
	public void dispatchMessage(Message msg) {
		if (msg.callback != null) {
			handleCallback(msg);
		} else {
			if (mCallback != null) {
				if (mCallback.handleMessage(msg)) {
					return;
				}
			}
			handleMessage(msg);
		}
	}

	/**
	 * Default constructor associates this handler with the queue for the current thread.
	 * 
	 * If there isn't one, this handler won't be able to receive messages.
	 */
	public Handler() {
		mLooper = Looper.myLooper();
		if (mLooper == null) {
			throw new RuntimeException("Can't create handler inside thread that has not called Looper.prepare()");
		}
		mMessageQueue = mLooper.mMessageQueue;
		mCallback = null;
	}

	/**
	 * Constructor associates this handler with the queue for the current thread and takes a
	 * callback interface in which you can handle messages.
	 */
	public Handler(Callback callback) {
		mLooper = Looper.myLooper();
		if (mLooper == null) {
			throw new RuntimeException("Can't create handler inside thread that has not called Looper.prepare()");
		}
		mMessageQueue = mLooper.mMessageQueue;
		mCallback = callback;
	}

	/**
	 * Use the provided queue instead of the default one.
	 */
	public Handler(Looper looper) {
		mLooper = looper;
		mMessageQueue = looper.mMessageQueue;
		mCallback = null;
	}

	/**
	 * Use the provided queue instead of the default one and take a callback interface in which to
	 * handle messages.
	 */
	public Handler(Looper looper, Callback callback) {
		mLooper = looper;
		mMessageQueue = looper.mMessageQueue;
		mCallback = callback;
	}

	/**
	 * Returns a string representing the name of the specified message. The default implementation
	 * will either return the class name of the message callback if any, or the hexadecimal
	 * representation of the message "what" field.
	 * 
	 * @param message The message whose name is being queried
	 */
	public String getMessageName(Message message) {
		if (message.callback != null) {
			return message.callback.getClass().getName();
		}
		return "0x" + Integer.toHexString(message.what);
	}

	/**
	 * Returns a new {@link mindroid.os.Message Message} from the global message pool. More
	 * efficient than creating and allocating new instances. The retrieved message has its handler
	 * set to this instance (Message.target == this). If you don't want that facility, just call
	 * Message.obtain() instead.
	 */
	public final Message obtainMessage() {
		return Message.obtain(this);
	}

	/**
	 * Same as {@link #obtainMessage()}, except that it also sets the what member of the returned
	 * Message.
	 * 
	 * @param what Value to assign to the returned Message.what field.
	 * @return A Message from the global message pool.
	 */
	public final Message obtainMessage(int what) {
		return Message.obtain(this, what);
	}

	/**
	 * 
	 * Same as {@link #obtainMessage()}, except that it also sets the what and obj members of the
	 * returned Message.
	 * 
	 * @param what Value to assign to the returned Message.what field.
	 * @param obj Value to assign to the returned Message.obj field.
	 * @return A Message from the global message pool.
	 */
	public final Message obtainMessage(int what, Object obj) {
		return Message.obtain(this, what, obj);
	}

	/**
	 * 
	 * Same as {@link #obtainMessage()}, except that it also sets the what, arg1 and arg2 members of
	 * the returned Message.
	 * 
	 * @param what Value to assign to the returned Message.what field.
	 * @param arg1 Value to assign to the returned Message.arg1 field.
	 * @param arg2 Value to assign to the returned Message.arg2 field.
	 * @return A Message from the global message pool.
	 */
	public final Message obtainMessage(int what, int arg1, int arg2) {
		return Message.obtain(this, what, arg1, arg2);
	}

	/**
	 * 
	 * Same as {@link #obtainMessage()}, except that it also sets the what, obj, arg1,and arg2
	 * values on the returned Message.
	 * 
	 * @param what Value to assign to the returned Message.what field.
	 * @param arg1 Value to assign to the returned Message.arg1 field.
	 * @param arg2 Value to assign to the returned Message.arg2 field.
	 * @param obj Value to assign to the returned Message.obj field.
	 * @return A Message from the global message pool.
	 */
	public final Message obtainMessage(int what, int arg1, int arg2, Object obj) {
		return Message.obtain(this, what, arg1, arg2, obj);
	}

	/**
	 * Causes the Runnable r to be added to the message queue. The runnable will be run on the
	 * thread to which this handler is attached.
	 * 
	 * @param runnable The Runnable that will be executed.
	 * 
	 * @return Returns true if the Runnable was successfully placed in to the message queue. Returns
	 * false on failure, usually because the looper processing the message queue is exiting.
	 */
	public final boolean post(Runnable runnable) {
		return sendMessageDelayed(getPostMessage(runnable), 0);
	}

	/**
	 * Causes the Runnable r to be added to the message queue, to be run at a specific time given by
	 * <var>uptimeMillis</var>. <b>The time-base is {@link mindroid.os.SystemClock#uptimeMillis}
	 * .</b> The runnable will be run on the thread to which this handler is attached.
	 * 
	 * @param runnable The Runnable that will be executed.
	 * @param uptimeMillis The absolute time at which the callback should run, using the
	 * {@link mindroid.os.SystemClock#uptimeMillis} time-base.
	 * 
	 * @return Returns true if the Runnable was successfully placed in to the message queue. Returns
	 * false on failure, usually because the looper processing the message queue is exiting. Note
	 * that a result of true does not mean the Runnable will be processed -- if the looper is quit
	 * before the delivery time of the message occurs then the message will be dropped.
	 */
	public final boolean postAtTime(Runnable runnable, long uptimeMillis) {
		return sendMessageAtTime(getPostMessage(runnable), uptimeMillis);
	}

	/**
	 * Causes the Runnable r to be added to the message queue, to be run at a specific time given by
	 * <var>uptimeMillis</var>. <b>The time-base is {@link mindroid.os.SystemClock#uptimeMillis}
	 * .</b> The runnable will be run on the thread to which this handler is attached.
	 * 
	 * @param runnable The Runnable that will be executed.
	 * @param uptimeMillis The absolute time at which the callback should run, using the
	 * {@link mindroid.os.SystemClock#uptimeMillis} time-base.
	 * 
	 * @return Returns true if the Runnable was successfully placed in to the message queue. Returns
	 * false on failure, usually because the looper processing the message queue is exiting. Note
	 * that a result of true does not mean the Runnable will be processed -- if the looper is quit
	 * before the delivery time of the message occurs then the message will be dropped.
	 * 
	 * @see mindroid.os.SystemClock#uptimeMillis
	 */
	public final boolean postAtTime(Runnable runnable, Object token, long uptimeMillis) {
		return sendMessageAtTime(getPostMessage(runnable, token), uptimeMillis);
	}

	/**
	 * Causes the Runnable r to be added to the message queue, to be run after the specified amount
	 * of time elapses. The runnable will be run on the thread to which this handler is attached.
	 * 
	 * @param runnable The Runnable that will be executed.
	 * @param delayMillis The delay (in milliseconds) until the Runnable will be executed.
	 * 
	 * @return Returns true if the Runnable was successfully placed in to the message queue. Returns
	 * false on failure, usually because the looper processing the message queue is exiting. Note
	 * that a result of true does not mean the Runnable will be processed -- if the looper is quit
	 * before the delivery time of the message occurs then the message will be dropped.
	 */
	public final boolean postDelayed(Runnable runnable, long delayMillis) {
		return sendMessageDelayed(getPostMessage(runnable), delayMillis);
	}

	/**
	 * Remove any pending posts of Runnable r that are in the message queue.
	 */
	public final boolean removeCallbacks(Runnable runnable) {
		return mMessageQueue.removeMessages(this, runnable, null);
	}

	/**
	 * Remove any pending posts of Runnable <var>r</var> with Object <var>token</var> that are in
	 * the message queue. If <var>token</var> is null, all callbacks will be removed.
	 */
	public final boolean removeCallbacks(Runnable runnable, Object token) {
		return mMessageQueue.removeMessages(this, runnable, token);
	}

	/**
	 * Pushes a message onto the end of the message queue after all pending messages before the
	 * current time. It will be received in {@link #handleMessage}, in the thread attached to this
	 * handler.
	 * 
	 * @return Returns true if the message was successfully placed in to the message queue. Returns
	 * false on failure, usually because the looper processing the message queue is exiting.
	 */
	public final boolean sendMessage(Message message) {
		return sendMessageDelayed(message, 0);
	}

	/**
	 * Sends a Message containing only the what value.
	 * 
	 * @return Returns true if the message was successfully placed in to the message queue. Returns
	 * false on failure, usually because the looper processing the message queue is exiting.
	 */
	public final boolean sendEmptyMessage(int what) {
		return sendEmptyMessageDelayed(what, 0);
	}

	/**
	 * Sends a Message containing only the what value, to be delivered after the specified amount of
	 * time elapses.
	 * 
	 * @see #sendMessageDelayed(mindroid.os.Message, long)
	 * 
	 * @return Returns true if the message was successfully placed in to the message queue. Returns
	 * false on failure, usually because the looper processing the message queue is exiting.
	 */
	public final boolean sendEmptyMessageDelayed(int what, long delayMillis) {
		Message message = Message.obtain();
		message.what = what;
		return sendMessageDelayed(message, delayMillis);
	}

	/**
	 * Sends a Message containing only the what value, to be delivered at a specific time.
	 * 
	 * @see #sendMessageAtTime(mindroid.os.Message, long)
	 * 
	 * @return Returns true if the message was successfully placed in to the message queue. Returns
	 * false on failure, usually because the looper processing the message queue is exiting.
	 */
	public final boolean sendEmptyMessageAtTime(int what, long uptimeMillis) {
		Message message = Message.obtain();
		message.what = what;
		return sendMessageAtTime(message, uptimeMillis);
	}

	/**
	 * Enqueue a message into the message queue after all pending messages before (current time +
	 * delayMillis). You will receive it in {@link #handleMessage}, in the thread attached to this
	 * handler.
	 * 
	 * @return Returns true if the message was successfully placed in to the message queue. Returns
	 * false on failure, usually because the looper processing the message queue is exiting. Note
	 * that a result of true does not mean the message will be processed -- if the looper is quit
	 * before the delivery time of the message occurs then the message will be dropped.
	 */
	public final boolean sendMessageDelayed(Message message, long delayMillis) {
		if (delayMillis < 0) {
			delayMillis = 0;
		}
		return sendMessageAtTime(message, SystemClock.uptimeMillis() + delayMillis);
	}

	/**
	 * Enqueue a message into the message queue after all pending messages before the absolute time
	 * (in milliseconds) <var>uptimeMillis</var>. <b>The time-base is
	 * {@link mindroid.os.SystemClock#uptimeMillis}.</b> You will receive it in
	 * {@link #handleMessage}, in the thread attached to this handler.
	 * 
	 * @param uptimeMillis The absolute time at which the message should be delivered, using the
	 * {@link mindroid.os.SystemClock#uptimeMillis} time-base.
	 * 
	 * @return Returns true if the message was successfully placed in to the message queue. Returns
	 * false on failure, usually because the looper processing the message queue is exiting. Note
	 * that a result of true does not mean the message will be processed -- if the looper is quit
	 * before the delivery time of the message occurs then the message will be dropped.
	 */
	public boolean sendMessageAtTime(Message message, long uptimeMillis) {
		boolean sent = false;
		MessageQueue queue = mMessageQueue;
		if (queue != null) {
			message.target = this;
			sent = queue.enqueueMessage(message, uptimeMillis);
		} else {
			RuntimeException e = new RuntimeException(this + " sendMessageAtTime() called with no mQueue");
			Log.w(LOG_TAG, e.getMessage(), e);
		}
		return sent;
	}

	/**
	 * Remove any pending posts of messages with code 'what' that are in the message queue.
	 */
	public final boolean removeMessages(int what) {
		return mMessageQueue.removeMessages(this, what, null);
	}

	/**
	 * Remove any pending posts of messages with code 'what' and whose obj is 'object' that are in
	 * the message queue. If <var>object</var> is null, all messages will be removed.
	 */
	public final boolean removeMessages(int what, Object object) {
		return mMessageQueue.removeMessages(this, what, object);
	}

	/**
	 * Remove any pending posts of callbacks and sent messages whose <var>obj</var> is
	 * <var>token</var>. If <var>token</var> is null, all callbacks and messages will be removed.
	 */
	public final boolean removeCallbacksAndMessages(Object token) {
		return mMessageQueue.removeCallbacksAndMessages(this, token);
	}

	/**
	 * Check if there are any pending posts of messages with code 'what' in the message queue.
	 */
	public final boolean hasMessages(int what) {
		return mMessageQueue.hasMessages(this, what, null);
	}

	/**
	 * Check if there are any pending posts of messages with code 'what' and whose obj is 'object'
	 * in the message queue.
	 */
	public final boolean hasMessages(int what, Object object) {
		return mMessageQueue.hasMessages(this, what, object);
	}

	/**
	 * Check if there are any pending posts of messages with callback r in the message queue.
	 * 
	 * @hide
	 */
	public final boolean hasCallbacks(Runnable runnable) {
		return mMessageQueue.hasMessages(this, runnable, null);
	}

	// If we can get rid of this method, the handler need not remember its loop
	// we could instead export a getMessageQueue() method...
	public final Looper getLooper() {
		return mLooper;
	}

	public String toString() {
		return "Handler (" + getClass().getName() + ") {" + Integer.toHexString(System.identityHashCode(this)) + "}";
	}

	private static Message getPostMessage(Runnable runnable) {
		Message message = Message.obtain();
		message.callback = runnable;
		return message;
	}

	private static Message getPostMessage(Runnable runnable, Object token) {
		Message message = Message.obtain();
		message.obj = token;
		message.callback = runnable;
		return message;
	}

	private static void handleCallback(Message message) {
		message.callback.run();
	}
}
