/*
 * Copyright (C) 2012 Daniel Himmelein
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

package mindroid.util;

import mindroid.app.Service;
import mindroid.os.Handler;
import mindroid.util.CircularLogBuffer.LogMessage;

public class ConsoleLogger extends Service {
	private static final String LOG_TAG = "ConsoleLogger";
	private CircularLogBuffer mLogBuffer = Log.getLogBuffer();
	private Thread mThread;
	private volatile boolean mRunning = true;
	private int mPrio = Log.DEBUG;
	private boolean mTimestamp = true;

	public void onCreate() {
		Log.i(LOG_TAG, "onCreate");
		
		mThread = new Thread() {
			public void run() {
				mRunning = true;
				while (mRunning) {
					LogMessage logMessage = mLogBuffer.getLogMessage(mPrio);
					if (logMessage != null) {
						System.out.println(logMessage.toString(mTimestamp));
					}
				}
			}
		};
	}

	public void onStart(int startId) {
		Log.i(LOG_TAG, "onStart: " + startId);
		mThread.start();
	}

	public void onDestroy() {
		Log.i(LOG_TAG, "onDestroy");
		mRunning = false;
		Log.i(LOG_TAG, "notify");
		try {
			mThread.join();
		} catch (InterruptedException e) {
		}
	}

	public Handler onBind() {
		return null;
	}
}
