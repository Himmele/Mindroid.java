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

import java.util.ArrayList;
import mindroid.app.Service;
import mindroid.content.Intent;
import mindroid.os.IBinder;
import mindroid.util.LogBuffer.LogMessage;

public class ConsoleLogger extends Service {
	private static final String LOG_TAG = "ConsoleLogger";
	private LoggerThread mMainLogThread = null;
	private LoggerThread mTaskManagerLogThread = null;
	private ArrayList mLogIds;

	class LoggerThread extends Thread {
		private LogBuffer mLogBuffer;
		private boolean mUseTimestamps;
		private int mPriority;
		
		public LoggerThread(String name, int logId, boolean useTimeStamps, int priority) {
			super(name);
			setPriority(MIN_PRIORITY);
			mLogBuffer = Log.getLogBuffer(logId);
			mUseTimestamps = useTimeStamps;
			mPriority = priority;
		}
		
		public void run() {
			while (!isInterrupted()) {
				LogMessage logMessage;
				try {
					logMessage = mLogBuffer.dequeue(mPriority);
				} catch (InterruptedException e) {
					break;
				}
				if (logMessage != null) {
					System.out.println(logMessage.toString(mUseTimestamps));
				}
			}
		}
		
		public void enableTimestamps(boolean useTimestamps) {
			mUseTimestamps = useTimestamps;
			mLogBuffer.resume();
		}
		
		public void setLogPriority(int priority) {
			mPriority = priority;
			mLogBuffer.resume();
		}
	}

	public void onCreate() {
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		mLogIds = intent.getIntegerArrayListExtra("logIds");
		if ((mLogIds == null) || mLogIds.contains(new Integer(Log.LOG_ID_MAIN))) {
			if (mMainLogThread == null) {
				boolean useTimestamps = intent.getBooleanExtra("timestamps", false);
				int priority = intent.getIntExtra("priority", Log.ERROR);
				mMainLogThread = new LoggerThread(LOG_TAG, Log.LOG_ID_MAIN, useTimestamps, priority);
				mMainLogThread.start();
			} else {
				boolean useTimestamps = intent.getBooleanExtra("timestamps", false);
				int priority = intent.getIntExtra("priority", Log.ERROR);
				mMainLogThread.enableTimestamps(useTimestamps);
				mMainLogThread.setLogPriority(priority);
			}
		}
		
		if ((mLogIds != null) && mLogIds.contains(new Integer(Log.LOG_ID_TASK_MANAGER))) {
			if (mTaskManagerLogThread == null) {
				mTaskManagerLogThread = new LoggerThread(LOG_TAG, Log.LOG_ID_TASK_MANAGER, true, Log.DEBUG);
				mTaskManagerLogThread.start();
			}
		}
		
        return 0;
    }

	public void onDestroy() {
		if (mMainLogThread != null) {
			mMainLogThread.interrupt();
			try {
				mMainLogThread.join();
			} catch (InterruptedException e) {
			}
		}
		
		if (mTaskManagerLogThread != null) {
			mTaskManagerLogThread.interrupt();
			try {
				mTaskManagerLogThread.join();
			} catch (InterruptedException e) {
			}
		}
	}

	public IBinder onBind(Intent intent) {
		return null;
	}
}
