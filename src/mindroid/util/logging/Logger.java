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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import mindroid.app.Service;
import mindroid.content.Intent;
import mindroid.os.Bundle;
import mindroid.os.Environment;
import mindroid.os.IBinder;
import mindroid.os.IRemoteCallback;
import mindroid.os.RemoteException;
import mindroid.util.Log;
import mindroid.util.logging.LogBuffer.LogRecord;

public class Logger extends Service {
    private static final String LOG_TAG = "Logger";

    public static final String ACTION_LOG = "mindroid.util.logging.LOG";
    public static final String ACTION_DUMP_LOG = "mindroid.util.logging.DUMP_LOG";
    public static final String ACTION_FLUSH_LOG = "mindroid.util.logging.FLUSH_LOG";
    public static final String ACTION_CLEAR_LOG = "mindroid.util.logging.CLEAR_LOG";

    private Map<Integer, LogWorker> mLogWorkers = new HashMap<>();

    class LogWorker extends Thread {
        private static final int JOIN_TIMEOUT = 10000; //ms

        private LogBuffer mLogBuffer;
        private int mLogPriority;
        private String[] mFlags;
        private ConsoleHandler mConsoleHandler;
        private FileHandler mFileHandler;
        private Handler mCustomHandler;

        public LogWorker(Bundle arguments) {
            super(LOG_TAG + " {" + arguments.getInt("logBuffer", Log.LOG_ID_MAIN) + "}");
            int logBuffer = arguments.getInt("logBuffer", Log.LOG_ID_MAIN);
            mLogBuffer = Log.getLogBuffer(logBuffer);
            if (mLogBuffer == null) {
                throw new IllegalArgumentException("Invalid log buffer: " + logBuffer);
            }
            mLogPriority = arguments.getInt("logPriority", Log.INFO);
            mFlags = arguments.getStringArray("logFlags");
            boolean consoleLogging = arguments.getBoolean("consoleLogging", false);
            if (consoleLogging) {
                mConsoleHandler = new ConsoleHandler();
                if (mFlags != null && Arrays.asList(mFlags).contains("timestamp")) {
                    mConsoleHandler.setFlag(ConsoleHandler.FLAG_TIMESTAMP);
                }
            } else {
                System.out.println("D/" + LOG_TAG + ": Console logging: disabled");
            }
            boolean fileLogging = arguments.getBoolean("fileLogging", false);
            if (fileLogging) {
                String directory = arguments.getString("logDirectory", Environment.getLogDirectory().getAbsolutePath());
                String fileName = arguments.getString("logFileName", "Log" + mLogBuffer.getId() + "-%g.log");
                int fileLimit = arguments.getInt("logFileLimit", 262144);
                int fileCount = arguments.getInt("logFileCount", 4);
                int bufferSize = arguments.getInt("logBufferSize", 0);
                int dataVolumeLimit = arguments.getInt("logDataVolumeLimit", 0);
                try {
                    mFileHandler = new FileHandler(directory + File.separator + fileName, fileLimit, fileCount, true,
                            bufferSize, dataVolumeLimit);
                } catch (IOException e) {
                    System.out.println("E/" + LOG_TAG + ": File logging error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            final String customHandler = arguments.getString("customLogging", null);
            if (customHandler != null && customHandler.length() > 0) {
                try {
                    Class<?> clazz = Class.forName(customHandler);
                    mCustomHandler = (Handler) clazz.newInstance();
                } catch (Exception e) {
                    System.out.println("E/" + LOG_TAG + ": Cannot create custom handler \'" + customHandler + "\': " + e.getMessage());
                    e.printStackTrace();
                } catch (LinkageError e) {
                    System.out.println("E/" + LOG_TAG + ": Linkage error: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            if (mConsoleHandler == null && mFileHandler == null && mCustomHandler == null) {
                throw new IllegalStateException("Invalid logging configuration");
            }
        }

        public void run() {
            open();

            while (!isInterrupted()) {
                LogRecord logRecord;
                try {
                    logRecord = mLogBuffer.take(mLogPriority);
                } catch (InterruptedException e) {
                    break;
                }
                if (logRecord != null) {
                    if (mConsoleHandler != null) {
                        mConsoleHandler.publish(logRecord);
                    }
                    if (mFileHandler != null) {
                        mFileHandler.publish(logRecord);
                    }
                    if (mCustomHandler != null) {
                        mCustomHandler.publish(logRecord);
                    }
                }
            }

            close();
        }

        private void open() {
        }

        private void close() {
            if (mConsoleHandler != null) {
                mConsoleHandler.close();
                mConsoleHandler = null;
            }

            if (mFileHandler != null) {
                mFileHandler.close();
                mFileHandler = null;
            }

            if (mCustomHandler != null) {
                mCustomHandler.close();
                mCustomHandler = null;
            }
        }

        void quit() {
            interrupt();
            mLogBuffer.quit();
            try {
                join(JOIN_TIMEOUT);
                if (isAlive()) {
                    System.out.println("E/" + LOG_TAG + ": Cannot join thread " + getName());
                    mindroid.lang.Runtime.getRuntime().exit(-1, "Cannot join thread " + getName());
                }
            } catch (InterruptedException e) {
            }
        }

        boolean dumpLog(String fileName) {
            if (mFileHandler != null) {
                return mFileHandler.dump(fileName);
            } else {
                return false;
            }
        }

        void flush() {
            if (mFileHandler != null) {
                mFileHandler.flush();
            }
        }

        void clear() {
            if (mFileHandler != null) {
                mFileHandler.clear();
            }
        }
    }

    public void onCreate() {
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (ACTION_LOG.equals(action)) {
            stopLogging(intent.getExtras());
            startLogging(intent.getExtras());
        } else if (ACTION_DUMP_LOG.equals(action)) {
            dumpLog(intent.getExtras());
        } else if (ACTION_FLUSH_LOG.equals(action)) {
            flushLog(intent.getExtras());
        } else if (ACTION_CLEAR_LOG.equals(action)) {
            clearLog(intent.getExtras());
        }

        return 0;
    }

    public void onDestroy() {
        System.out.println("D/" + LOG_TAG + ": Flushing logs");
        Iterator<Map.Entry<Integer, LogWorker>> itr = mLogWorkers.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<Integer, LogWorker> entry = itr.next();
            LogWorker logWorker = entry.getValue();
            logWorker.flush();
            logWorker.quit();
            itr.remove();
        }
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startLogging(Bundle arguments) {
        final int logBuffer = arguments.getInt("logBuffer", Log.LOG_ID_MAIN);
        final int threadPriority = arguments.getInt("threadPriority", Thread.MIN_PRIORITY);
        if (!mLogWorkers.containsKey(logBuffer)) {
            System.out.println("D/" + LOG_TAG + ": Starting logging {" + logBuffer + "}");
            try {
                LogWorker logWorker = new LogWorker(arguments);
                mLogWorkers.put(logBuffer, logWorker);
                logWorker.setPriority(threadPriority);
                logWorker.start();
                System.out.println("D/" + LOG_TAG + ": Logging has been started {" + logBuffer + "}");
            } catch (IllegalArgumentException e) {
                System.out.println("D/" + LOG_TAG + ": Failed to start logging {" + logBuffer + "}");
                e.printStackTrace();
            } catch (IllegalStateException e) {
                System.out.println("D/" + LOG_TAG + ": Logging: disabled {" + logBuffer + "}");
            }
        }
    }

    private void stopLogging(Bundle arguments) {
        final int logBuffer = arguments.getInt("logBuffer", Log.LOG_ID_MAIN);
        if (mLogWorkers.containsKey(logBuffer)) {
            System.out.println("D/" + LOG_TAG + ": Stopping logging {" + logBuffer + "}");
            LogWorker logWorker = mLogWorkers.get(logBuffer);
            logWorker.quit();
            mLogWorkers.remove(logBuffer);
            System.out.println("D/" + LOG_TAG + ": Logging has been stopped {" + logBuffer + "}");
        }
    }

    private void dumpLog(Bundle arguments) {
        final int logBuffer = arguments.getInt("logBuffer", Log.LOG_ID_MAIN);
        final String fileName = arguments.getString("fileName");
        IRemoteCallback callback = IRemoteCallback.Stub.asInterface(arguments.getBinder("binder"));
        if (mLogWorkers.containsKey(logBuffer)) {
            System.out.println("D/" + LOG_TAG + ": Dumping log to file " + fileName + " {" + logBuffer + "}");
            LogWorker logWorker = mLogWorkers.get(logBuffer);
            Bundle result = new Bundle();
            if (logWorker.dumpLog(fileName)) {
                System.out.println("D/" + LOG_TAG + ": Log has been dumped to file " + fileName + " {" + logBuffer + "}");
                result.putBoolean("result", true);
            } else {
                System.out.println("D/" + LOG_TAG + ": Failed to dump log to file " + fileName + " {" + logBuffer + "}");
                result.putBoolean("result", false);
            }
            if (callback != null) {
                try {
                    callback.sendResult(result);
                } catch (RemoteException ignore) {
                }
            }
        }
    }

    private void flushLog(Bundle arguments) {
        final int logBuffer = arguments.getInt("logBuffer", Log.LOG_ID_MAIN);
        if (mLogWorkers.containsKey(logBuffer)) {
            System.out.println("D/" + LOG_TAG + ": Flushing log {" + logBuffer + "}");
            LogWorker logWorker = mLogWorkers.get(logBuffer);
            logWorker.flush();
        }
    }

    private void clearLog(Bundle arguments) {
        final int logBuffer = arguments.getInt("logBuffer", Log.LOG_ID_MAIN);
        if (mLogWorkers.containsKey(logBuffer)) {
            System.out.println("D/" + LOG_TAG + ": Clearing log {" + logBuffer + "}");
            LogWorker logWorker = mLogWorkers.get(logBuffer);
            logWorker.clear();
        }
    }
}
