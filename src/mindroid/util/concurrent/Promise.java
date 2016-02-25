/*
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

package mindroid.util.concurrent;

import mindroid.os.SystemClock;

public class Promise implements Future {
	private Object mObject = null;
	private boolean mIsDone = false;
	private boolean mIsCancelled = false;

	public Promise() {
	}

	public synchronized boolean cancel() {
		if (!mIsDone && !mIsCancelled) {
			mIsCancelled = true;
			notify();
			return true;
		} else {
			return false;
		}
	}

	public synchronized boolean isCancelled() {
		return mIsCancelled;
	}

	public synchronized boolean isDone() {
		return mIsDone;
	}

	public synchronized Object get() throws CancellationException, ExecutionException, InterruptedException {
		while (!isDone()) {
			if (isCancelled()) {
				throw new CancellationException("Cancellation exception");
			}
			try {
				wait();
			} catch (InterruptedException e) {
				throw e;
			}
		}
		return mObject;
	}

	public synchronized Object get(long timeout) throws CancellationException, ExecutionException, TimeoutException, InterruptedException {
		long start = SystemClock.uptimeMillis();
		long duration = timeout;
		while (!isDone() && (duration > 0)) {
			if (isCancelled()) {
				throw new CancellationException("Cancellation exception");
			}
			try {
				wait(duration);
			} catch (InterruptedException e) {
				throw e;
			}
			duration = start + timeout - SystemClock.uptimeMillis();
		}
		if (!isDone() && !isCancelled()) {
			throw new TimeoutException("Future timed out");
		}
		return mObject;
	}

	public synchronized boolean set(Object object) {
		if (!mIsCancelled) {
			mObject = object;
			mIsDone = true;
			notify();
			return true;
		} else {
			return false;
		}
	}
}
