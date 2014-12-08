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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import mindroid.app.Service;
import mindroid.content.Intent;
import mindroid.os.IBinder;
import mindroid.util.LogBuffer.LogMessage;

public class Logger extends Service {
	private static final String LOG_TAG = "Logger";
	private static final int MAX_SYSOUT_LINE_LENGTH = 255;
	private static String LOG_SEPARATOR = "================================================================================";
	private LoggerThread mMainLogThread = null;
	private LoggerThread mTaskManagerLogThread = null;
	private ArrayList mLogIds;

	class LoggerThread extends Thread {
		private LogBuffer mLogBuffer;
		private boolean mUseTimestamps;
		private HashMap mTags;
		private int mPriority;
		private String mLogPath;
		private BufferedWriter mLogFileWriter;
		
		public LoggerThread(String name, int logId, boolean useTimeStamps, HashMap tags, int priority, String logPath) {
			super(name);
			mLogBuffer = Log.getLogBuffer(logId);
			mUseTimestamps = useTimeStamps;
			mTags = tags;
			mPriority = priority;
			if (logPath != null && logPath.length() > 0) {
				mLogPath = logPath;
			}
		}
		
		public void run() {
			open();
			
			while (!isInterrupted()) {
				LogMessage logMessage;
				try {
					logMessage = mLogBuffer.take(mPriority);
				} catch (InterruptedException e) {
					break;
				}
				if ((logMessage != null) && (mTags == null || mTags.containsKey(logMessage.getTag()))) {
					String output = logMessage.toString(mUseTimestamps);
					println(output);
					write(output);
				}
			}
			
			close();
		}
		
		public void enableTimestamps(boolean useTimestamps) {
			mUseTimestamps = useTimestamps;
			mLogBuffer.resume();
		}
		
		public void setLogPriority(int priority) {
			mPriority = priority;
			mLogBuffer.resume();
		}
		
		private void println(String output) {
			for (int i = 0; i < output.length(); i += MAX_SYSOUT_LINE_LENGTH) {
				String o = output.substring(i, Math.min(output.length(), i + MAX_SYSOUT_LINE_LENGTH));
				System.out.print(o);
				System.out.flush();
			}
			System.out.println();
		}
		
		private void open() {
			if (mLogPath != null) {
				try {
					File logDirectory = new File(mLogPath).getParentFile();
					if (!logDirectory.exists()) {
						logDirectory.mkdir();
					}
					File logPath = new File(mLogPath);
					if (!logPath.exists()) {
						try {
							logPath.createNewFile();
							mLogFileWriter = new BufferedWriter(new FileWriter(logPath, true));
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						mLogFileWriter = new BufferedWriter(new FileWriter(logPath, true));
						mLogFileWriter.newLine();
						mLogFileWriter.write(LOG_SEPARATOR, 0, LOG_SEPARATOR.length());
						mLogFileWriter.newLine();
						mLogFileWriter.newLine();
						mLogFileWriter.flush();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		private void close() {
			if (mLogFileWriter != null) {
				try {
					mLogFileWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		private void write(String output) {
			if (mLogFileWriter != null) {
				try {
					mLogFileWriter.write(output, 0, output.length());
					mLogFileWriter.newLine();
					mLogFileWriter.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void onCreate() {
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		int threadPriority = intent.getIntExtra("threadPriority", Thread.MIN_PRIORITY);
		mLogIds = intent.getIntegerArrayListExtra("logIds");
		if ((mLogIds == null) || mLogIds.contains(new Integer(Log.LOG_ID_MAIN))) {
			if (mMainLogThread == null) {
				boolean useTimestamps = intent.getBooleanExtra("timestamps", false);
				
				HashMap tags = null;
				ArrayList tagsArray = intent.getStringArrayListExtra("tags");
				if ((tagsArray != null) && !tagsArray.contains("*")) {
					tags = new HashMap();
					for (int i = 0; i < tagsArray.size(); i++) {
						String tag = (String) tagsArray.get(i);
						if (tag.length() > 0) {
							tags.put(tag, new Object());
						}
					}
				}
				
				int priority = intent.getIntExtra("priority", Log.ERROR);
				String logDirectory = intent.getStringExtra("logDirectory");
				String logFileName = intent.getStringExtra("logFileName");
				String logPath = null;
				if (logDirectory != null && logDirectory.length() > 0) {
					if (logFileName != null && logFileName.length() > 0) {
						logPath = logDirectory + File.separator + logFileName;
					}
				}
				
				mMainLogThread = new LoggerThread(LOG_TAG, Log.LOG_ID_MAIN, useTimestamps, tags, priority, logPath);
				mMainLogThread.setPriority(threadPriority);
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
				mTaskManagerLogThread = new LoggerThread(LOG_TAG, Log.LOG_ID_TASK_MANAGER, true, null, Log.DEBUG, null);
				mTaskManagerLogThread.setPriority(threadPriority);
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
