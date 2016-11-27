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

package mindroid.util.logging;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import mindroid.app.Service;
import mindroid.content.Intent;
import mindroid.os.Environment;
import mindroid.os.IBinder;
import mindroid.util.Log;
import mindroid.util.logging.LogBuffer.LogRecord;

public class EventLogger extends Service {
    private static final String LOG_TAG = "EventLogger";
    private EventLoggerThread mEventLoggerThread = null;

    class EventLoggerThread extends Thread {
        private LogBuffer mLogBuffer;
        private int mPriority;
        private String mLogDirectory;
        private String mLogFileName;
        private int mLogFileSizeLimit;
        private int mLogFileCount;
        private FileHandler mFileHander;

        public EventLoggerThread(int priority, String logDirectory, String logFileName, int logFileSizeLimit, int logFileCount) {
            super(LOG_TAG);
            mPriority = priority;
            mLogBuffer = Log.getLogBuffer(Log.LOG_ID_EVENTS);
            mLogDirectory = logDirectory;
            mLogFileName = logFileName;
            mLogFileSizeLimit = logFileSizeLimit;
            mLogFileCount = logFileCount;
        }

        public void run() {
            open();

            while (!isInterrupted()) {
                LogRecord logMessage;
                try {
                    logMessage = mLogBuffer.take(mPriority);
                } catch (InterruptedException e) {
                    break;
                }
                if (logMessage != null) {
                    if (mFileHander != null) {
                        mFileHander.publish(logMessage);
                    }
                }
            }

            close();
        }

        private void open() {
            try {
                mFileHander = new FileHandler(mLogDirectory + File.separator + mLogFileName, mLogFileSizeLimit, mLogFileCount, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void close() {
            if (mFileHander != null) {
                mFileHander.close();
                mFileHander = null;
            }
        }
        
        void quit() {
            interrupt();
            mLogBuffer.quit();
            try {
                join();
            } catch (InterruptedException e) {
            }
        }
    }

    public void onCreate() {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        int threadPriority = intent.getIntExtra("threadPriority", Thread.MIN_PRIORITY);
        ArrayList logBuffers = intent.getStringArrayListExtra("logBuffers");
        if (logBuffers != null) {
            if (logBuffers.contains("events")) {
                String logDirectory = Environment.getLogDirectory().getAbsolutePath();
                String logFileName = intent.getStringExtra("eventLogFileName");
                int logFileSizeLimit = intent.getIntExtra("eventLogFileSizeLimit", 262144);
                int logFileCount = intent.getIntExtra("eventLogFileCount", 4);
                boolean log = (logDirectory != null && logDirectory.length() > 0 && logFileName != null && logFileName.length() > 0);

                if (log) {
                    mEventLoggerThread = new EventLoggerThread(Log.DEBUG, logDirectory, logFileName, logFileSizeLimit, logFileCount);
                    mEventLoggerThread.setPriority(threadPriority);
                    mEventLoggerThread.start();
                } else {
                    Log.e(LOG_TAG, "Invalid event logger configuration");
                }
            }
        }

        return 0;
    }

    public void onDestroy() {
        if (mEventLoggerThread != null) {
            mEventLoggerThread.quit();
        }
    }

    public IBinder onBind(Intent intent) {
        return null;
    }
}
