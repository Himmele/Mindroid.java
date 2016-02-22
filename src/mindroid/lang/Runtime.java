/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2014 Daniel Himmelein
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

package mindroid.lang;

import java.util.ArrayList;
import java.util.List;

import mindroid.os.Message;

/**
 * Allows Java applications to interface with the environment in which they are running.
 * Applications can not create an instance of this class, but they can get a singleton instance by
 * invoking {@link #getRuntime()}.
 */
public class Runtime {
	/**
	 * Holds the Singleton global instance of Runtime.
	 */
	private static final Runtime sRuntime = new Runtime();

	/**
	 * Holds the list of threads to run when the VM terminates
	 */
	private List mShutdownHooks = new ArrayList();

	/**
	 * Reflects whether we are already shutting down the VM.
	 */
	private boolean mShuttingDown;

	/**
	 * Prevent this class from being instantiated.
	 */
	private Runtime() {
	}

	/**
	 * Returns the single {@code Runtime} instance.
	 * 
	 * @return the {@code Runtime} object for the current application.
	 */
	public static Runtime getRuntime() {
		return sRuntime;
	}

	public void addShutdownHook(Message hook) {
		if (hook == null) {
			throw new NullPointerException("hook == null");
		}

		if (mShuttingDown) {
			throw new IllegalStateException("VM already shutting down");
		}

		synchronized (mShutdownHooks) {
			if (mShutdownHooks.contains(hook)) {
				throw new IllegalArgumentException("Hook already registered");
			}

			mShutdownHooks.add(hook);
		}
	}

	public boolean removeShutdownHook(Message hook) {
		if (hook == null) {
			throw new NullPointerException("hook == null");
		}

		if (mShuttingDown) {
			throw new IllegalStateException("VM already shutting down");
		}

		synchronized (mShutdownHooks) {
			return mShutdownHooks.remove(hook);
		}
	}

	/**
	 * Causes the Mindroid application framework to stop running and the program to exit. The exit
	 * code is set to the arg1 argument of the shutdown hook message. The exit reason is set to the
	 * obj argument of the shutdown hook message.
	 * 
	 * @param code the return code. By convention, non-zero return codes indicate abnormal
	 * terminations.
	 * @param reason the return reason.
	 */
	public void exit(int code) {
		exit(code, null);
	}

	public void exit(int code, String reason) {
		exit(code, reason, 0);
	}

	public void exit(int code, String reason, int delay) {
		synchronized (this) {
			if (!mShuttingDown) {
				mShuttingDown = true;

				Message[] hooks;
				synchronized (mShutdownHooks) {
					hooks = new Message[mShutdownHooks.size()];
					mShutdownHooks.toArray(hooks);
				}

				for (int i = 0; i < hooks.length; i++) {
					hooks[i].arg1 = code;
					hooks[i].arg2 = delay;
					hooks[i].obj = reason;
					try {
						hooks[i].sendToTarget();
					} catch (Exception e) {
						// Ignore exception.
					}
				}
			}
		}
	}

	/**
	 * Causes the VM to stop running, and the program to exit. Neither shutdown hooks nor finalizers
	 * are run before.
	 * 
	 * @param code the return code. By convention, non-zero return codes indicate abnormal
	 * terminations.
	 * @see #addShutdownHook(Message)
	 * @see #removeShutdownHook(Message)
	 */
	public void halt(int code) {
		System.exit(code);
	}
}
