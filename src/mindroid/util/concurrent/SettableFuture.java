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

public class SettableFuture {
	private Object mObject = null;
	private boolean mIsDone = false;
	
	public SettableFuture() {
	}
	
	public synchronized boolean isDone() {
		return mIsDone;
	}
	
	public synchronized Object get() {
		while (!isDone()) {
			try {
				wait();
			} catch (InterruptedException e) {
				throw new CancellationException("Binder transaction aborted");
			}
		}
		return mObject;
	}
	
	public synchronized Object get(long timeout) {
		long startTime = System.currentTimeMillis();
		long endTime = startTime;
		while (!isDone() && (endTime - startTime < timeout)) {
			try {
				wait(timeout - (endTime - startTime));
			} catch (InterruptedException e) {
				throw new CancellationException("Binder transaction aborted");
			}
			endTime = System.currentTimeMillis();
		}
		return mObject;
	}
	
	public synchronized void set(Object object) {
		mObject = object;
		mIsDone = true;
		notify();
	}
}
