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

public class SettableFuture implements Future {
	private Object mObject = null;
	private boolean mIsDone = false;
	private boolean mIsCancelled = false;
	
	public SettableFuture() {
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
				throw new CancellationException("Binder transaction error");
			}
			try {
				wait();
			} catch (InterruptedException e) {
				throw e;
			}
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
