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

import mindroid.util.concurrent.Future;

/**
 * Defines a message containing a description and arbitrary data object that can be sent to a
 * {@link Handler}. This object contains two extra int fields and an extra object field that allow
 * you to not do allocations in many cases.
 * 
 * <p class="note">
 * While the constructor of Message is public, the best way to get one of these is to call
 * {@link #obtain Message.obtain()} or one of the {@link Handler#obtainMessage
 * Handler.obtainMessage()} methods, which will pull them from a pool of recycled objects.
 * </p>
 */
public final class Message {
	/**
	 * User-defined message code so that the recipient can identify what this message is about. Each
	 * {@link Handler} has its own name-space for message codes, so you do not need to worry about
	 * yours conflicting with other handlers.
	 */
	public int what;

	/**
	 * arg1 and arg2 are lower-cost alternatives to using {@link #setData(Bundle) setData()} if you
	 * only need to store a few integer values.
	 */
	public int arg1;

	/**
	 * arg1 and arg2 are lower-cost alternatives to using {@link #setData(Bundle) setData()} if you
	 * only need to store a few integer values.
	 */
	public int arg2;

	/**
	 * An arbitrary object to send to the recipient.
	 */
	public Object obj;

	/*package*/ static final int FLAG_IN_USE = 1 << 0;

	/*package*/ int flags;

	/*package*/ long when;

	/*package*/ Bundle data;

	/*package*/ Handler target;

	/*package*/ Runnable callback;

	Message nextMessage;

	private static final Object sMessagePoolSync = new Object();
	private static Message sMessagePool;
	private static int sMessagePoolSize = 0;

	private static final int MAX_MESSAGE_POOL_SIZE = 42;

	/**
	 * Return a new Message instance from the global pool. Allows us to avoid allocating new objects
	 * in many cases.
	 */
	public static Message obtain() {
		synchronized (sMessagePoolSync) {
			if (sMessagePool != null) {
				Message message = sMessagePool;
				sMessagePool = message.nextMessage;
				message.nextMessage = null;
				message.flags = 0;
				sMessagePoolSize--;
				return message;
			}
		}
		return new Message();
	}

	/**
	 * Same as {@link #obtain()}, but copies the values of an existing message (including its
	 * target) into the new one.
	 * 
	 * @param origMessage Original message to copy.
	 * @return A Message object from the global pool.
	 */
	public static Message obtain(Message origMessage) {
		Message message = obtain();
		message.what = origMessage.what;
		message.arg1 = origMessage.arg1;
		message.arg2 = origMessage.arg2;
		message.obj = origMessage.obj;
		if (origMessage.data != null) {
			message.data = new Bundle(origMessage.data);
		}
		message.target = origMessage.target;
		message.callback = origMessage.callback;

		return message;
	}

	/**
	 * Same as {@link #obtain()}, but sets the value for the <em>target</em> member on the Message
	 * returned.
	 * 
	 * @param handler Handler to assign to the returned Message object's <em>target</em> member.
	 * @return A Message object from the global pool.
	 */
	public static Message obtain(Handler handler) {
		Message message = obtain();
		message.target = handler;

		return message;
	}

	/**
	 * Same as {@link #obtain(Handler)}, but assigns a callback Runnable on the Message that is
	 * returned.
	 * 
	 * @param handler Handler to assign to the returned Message object's <em>target</em> member.
	 * @param callback Runnable that will execute when the message is handled.
	 * @return A Message object from the global pool.
	 */
	public static Message obtain(Handler handler, Runnable callback) {
		Message message = obtain();
		message.target = handler;
		message.callback = callback;

		return message;
	}

	/**
	 * Same as {@link #obtain()}, but sets the values for both <em>target</em> and <em>what</em>
	 * members on the Message.
	 * 
	 * @param handler Value to assign to the <em>target</em> member.
	 * @param what Value to assign to the <em>what</em> member.
	 * @return A Message object from the global pool.
	 */
	public static Message obtain(Handler handler, int what) {
		Message message = obtain();
		message.target = handler;
		message.what = what;

		return message;
	}

	/**
	 * Same as {@link #obtain()}, but sets the values of the <em>target</em>, <em>what</em>, and
	 * <em>obj</em> members.
	 * 
	 * @param handler The <em>target</em> value to set.
	 * @param what The <em>what</em> value to set.
	 * @param obj The <em>object</em> method to set.
	 * @return A Message object from the global pool.
	 */
	public static Message obtain(Handler handler, int what, Object obj) {
		Message message = obtain();
		message.target = handler;
		message.what = what;
		message.obj = obj;

		return message;
	}

	/**
	 * Same as {@link #obtain()}, but sets the values of the <em>target</em>, <em>what</em>,
	 * <em>arg1</em>, and <em>arg2</em> members.
	 * 
	 * @param handler The <em>target</em> value to set.
	 * @param what The <em>what</em> value to set.
	 * @param arg1 The <em>arg1</em> value to set.
	 * @param arg2 The <em>arg2</em> value to set.
	 * @return A Message object from the global pool.
	 */
	public static Message obtain(Handler handler, int what, int arg1, int arg2) {
		Message message = obtain();
		message.target = handler;
		message.what = what;
		message.arg1 = arg1;
		message.arg2 = arg2;

		return message;
	}

	/**
	 * Same as {@link #obtain()}, but sets the values of the <em>target</em>, <em>what</em>,
	 * <em>arg1</em>, <em>arg2</em>, and <em>obj</em> members.
	 * 
	 * @param handler The <em>target</em> value to set.
	 * @param what The <em>what</em> value to set.
	 * @param arg1 The <em>arg1</em> value to set.
	 * @param arg2 The <em>arg2</em> value to set.
	 * @param obj The <em>obj</em> value to set.
	 * @return A Message object from the global pool.
	 */
	public static Message obtain(Handler handler, int what, int arg1, int arg2, Object obj) {
		Message message = obtain();
		message.target = handler;
		message.what = what;
		message.arg1 = arg1;
		message.arg2 = arg2;
		message.obj = obj;

		return message;
	}

	/**
	 * Return a Message instance to the global pool. You MUST NOT touch the Message after calling
	 * this function -- it has effectively been freed.
	 */
	public void recycle() {
		if (result != null) {
			result.cancel();
		}

		flags = FLAG_IN_USE;
		what = 0;
		arg1 = 0;
		arg2 = 0;
		obj = null;
		when = 0;
		target = null;
		callback = null;
		data = null;
		result = null;

		synchronized (sMessagePoolSync) {
			if (sMessagePoolSize < MAX_MESSAGE_POOL_SIZE) {
				nextMessage = sMessagePool;
				sMessagePool = this;
				sMessagePoolSize++;
			}
		}
	}

	/**
	 * Make this message like otherMessage. Performs a shallow copy of the data field. Does not copy the linked
	 * list fields, nor the timestamp or target/callback of the original message.
	 */
	public void copyFrom(Message otherMessage) {
		this.flags = 0;
		this.what = otherMessage.what;
		this.arg1 = otherMessage.arg1;
		this.arg2 = otherMessage.arg2;
		this.obj = otherMessage.obj;
		if (otherMessage.data != null) {
			this.data = (Bundle) otherMessage.data.clone();
		} else {
			this.data = null;
		}
	}

	/**
	 * Return the targeted delivery time of this message, in milliseconds.
	 */
	public long getWhen() {
		return when;
	}

	public void setTarget(Handler target) {
		this.target = target;
	}

	/**
	 * Retrieve the a {@link mindroid.os.Handler Handler} implementation that will receive this
	 * message. The object must implement
	 * {@link mindroid.os.Handler#handleMessage(mindroid.os.Message) Handler.handleMessage()}. Each
	 * Handler has its own name-space for message codes, so you do not need to worry about yours
	 * conflicting with other handlers.
	 */
	public Handler getTarget() {
		return target;
	}

	/**
	 * Retrieve callback object that will execute when this message is handled. This object must
	 * implement Runnable. This is called by the <em>target</em> {@link Handler} that is receiving
	 * this Message to dispatch it. If not set, the message will be dispatched to the receiving
	 * Handler's {@link Handler#handleMessage(Message Handler.handleMessage())}.
	 */
	public Runnable getCallback() {
		return callback;
	}

	/**
	 * Obtains a Bundle of arbitrary data associated with this event, lazily creating it if
	 * necessary. Set this value by calling {@link #setData(Bundle)}.
	 * 
	 * @see #peekData()
	 * @see #setData(Bundle)
	 */
	public Bundle getData() {
		if (data == null) {
			data = new Bundle();
		}

		return data;
	}

	/**
	 * Like getData(), but does not lazily create the Bundle. A null is returned if the Bundle does
	 * not already exist. See {@link #getData} for further information on this.
	 * 
	 * @see #getData()
	 * @see #setData(Bundle)
	 */
	public Bundle peekData() {
		return data;
	}

	/**
	 * Sets a Bundle of arbitrary data values. Use arg1 and arg1 members as a lower cost way to send
	 * a few simple integer values, if you can.
	 * 
	 * @see #getData()
	 * @see #peekData()
	 */
	public void setData(Bundle data) {
		this.data = data;
	}

	/**
	 * Sends this Message to the Handler specified by {@link #getTarget}. Throws a null pointer
	 * exception if this field has not been set.
	 */
	public void sendToTarget() {
		target.sendMessage(this);
	}

	/*package*/ boolean isInUse() {
		return ((flags & FLAG_IN_USE) == FLAG_IN_USE);
	}

	/*package*/ void markInUse() {
		flags |= FLAG_IN_USE;
	}

	/**
	 * Constructor (but the preferred way to get a Message is to call {@link #obtain()
	 * Message.obtain()}).
	 */
	public Message() {
	}

	/**
	 * Optional result. The semantics of exactly how this is used are up to the sender and receiver.
	 * 
	 * @hide
	 */
	public Future result;
}
