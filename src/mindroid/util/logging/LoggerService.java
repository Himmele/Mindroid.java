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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mindroid.app.Service;
import mindroid.content.Context;
import mindroid.content.Intent;
import mindroid.content.SharedPreferences;
import mindroid.os.Bundle;
import mindroid.os.Environment;
import mindroid.os.IBinder;
import mindroid.os.IRemoteCallback;
import mindroid.os.RemoteException;
import mindroid.os.ServiceManager;
import mindroid.util.Log;
import mindroid.util.Properties;
import mindroid.util.concurrent.CancellationException;
import mindroid.util.concurrent.ExecutionException;
import mindroid.util.concurrent.Promise;
import mindroid.util.logging.LogBuffer.LogRecord;

public class LoggerService extends Service {
    private static final String LOG_TAG = "Logger";

    private LoggerThread mLogger = new LoggerThread();
    private Map<Integer, List<Handler>> mLogHandlers = new ConcurrentHashMap<>();
    private TestHandler mTestHandler;

    class LoggerThread extends Thread {
        private static final int JOIN_TIMEOUT = 10000; //ms

        private Promise<Object> mWaitForLogs;

        public LoggerThread() {
            super("Logger");
        }

        public void run() {
            Map<Integer, Promise<LogRecord>> loggers = new HashMap<>();

            while (!isInterrupted()) {
                synchronized (this) {
                    Iterator<Map.Entry<Integer, List<Handler>>> itr = mLogHandlers.entrySet().iterator();
                    while (itr.hasNext()) {
                        Map.Entry<Integer, List<Handler>> entry = itr.next();
                        final Integer id = entry.getKey();
                        if (!loggers.containsKey(id)) {
                            LogBuffer logBuffer = Log.getLogBuffer(id);
                            loggers.put(id, logBuffer.get());
                        }
                    }
                    mWaitForLogs = Promise.anyOf(loggers.values().toArray(new Promise<?>[loggers.values().size()]));
                }

                try {
                    mWaitForLogs.get();
                } catch (InterruptedException e) {
                    break;
                } catch (CancellationException | ExecutionException ignore) {
                }
                Iterator<Map.Entry<Integer, Promise<LogRecord>>> itr = loggers.entrySet().iterator();
                while (itr.hasNext()) {
                    Map.Entry<Integer, Promise<LogRecord>> entry = itr.next();
                    Promise<LogRecord> promise = entry.getValue();
                    if (promise.isDone()) {
                        if (!promise.isCompletedExceptionally()) {
                            LogRecord logRecord = null;
                            try {
                                logRecord = promise.get();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } catch (CancellationException | ExecutionException ignore) {
                            }
                            if (logRecord != null) {
                                List<Handler> handlers = mLogHandlers.get(entry.getKey());
                                if (handlers != null) {
                                    for (Handler handler : handlers) {
                                        handler.publish(logRecord);
                                    }
                                }
                            }
                        }
                        itr.remove();
                    }
                }
            }

            Log.println('I', LOG_TAG, "Logger has been shut down");
        }

        void reset() {
            synchronized (this) {
                if (mWaitForLogs != null) {
                    mWaitForLogs.complete(null);
                }
            }
        }

        void quit() {
            interrupt();
            try {
                join(JOIN_TIMEOUT);
            } catch (InterruptedException ignore) {
            }
            if (isAlive()) {
                Log.println('E', LOG_TAG, "Cannot join thread " + getName());
                mindroid.lang.Runtime.getRuntime().exit(-1, "Cannot join thread " + getName());
            }
        }
    }

    private final IBinder mBinder = new ILogger.Stub() {
        @Override
        public Promise<String> assumeThat(String tag, String message, long timeout) throws RemoteException {
            if (mTestHandler != null) {
                return mTestHandler.assumeThat(tag, message, timeout);
            } else {
                return new Promise<>(new ExecutionException());
            }
        }

        @Override
        public void mark() throws RemoteException {
            if (mTestHandler != null) {
                mTestHandler.mark();
            }
        }

        @Override
        public void reset() throws RemoteException {
            if (mTestHandler != null) {
                mTestHandler.reset();
            }
        }
    };

    public void onCreate() {
        SharedPreferences preferences = Environment.getSharedPreferences(Environment.getPreferencesDirectory(), "Logger.xml", 0);
        final int threadPriority = preferences.getInt("threadPriority", Thread.NORM_PRIORITY);
        mLogger.setPriority(threadPriority);
        final String integrationTesting = System.getProperty(Properties.INTEGRATION_TESTING);
        if (integrationTesting != null && Boolean.valueOf(integrationTesting)) {
            mTestHandler = new TestHandler();
            List<Handler> handlers = new ArrayList<>();
            handlers.add(mTestHandler);
            mLogHandlers.put(Log.LOG_ID_TEST, handlers);
        }
        mLogger.start();
        ServiceManager.addService(Context.LOGGER_SERVICE, mBinder);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (Logger.ACTION_LOG.equals(action)) {
            stopLogging(intent.getExtras());
            startLogging(intent.getExtras());
        } else if (Logger.ACTION_DUMP_LOG.equals(action)) {
            dumpLog(intent.getExtras());
        } else if (Logger.ACTION_FLUSH_LOG.equals(action)) {
            flushLog(intent.getExtras());
        } else if (Logger.ACTION_CLEAR_LOG.equals(action)) {
            clearLog(intent.getExtras());
        }

        return 0;
    }

    public void onDestroy() {
        ServiceManager.removeService(mBinder);
        mLogger.quit();
        if (mTestHandler != null) {
            mTestHandler.clear();
        }
        Log.println('D', LOG_TAG, "Flushing logs");
        Iterator<Map.Entry<Integer, List<Handler>>> itr = mLogHandlers.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<Integer, List<Handler>> entry = itr.next();
            LogBuffer logBuffer = Log.getLogBuffer(entry.getKey());
            logBuffer.reset();
            List<Handler> handlers = entry.getValue();
            for (Handler handler : handlers) {
                handler.flush();
                handler.close();
            }
            itr.remove();
        }
    }

    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void startLogging(Bundle arguments) {
        final int logBuffer = arguments.getInt("logBuffer", Log.LOG_ID_MAIN);
        if (!mLogHandlers.containsKey(logBuffer)) {
            Log.println('D', LOG_TAG, "Starting logging {" + logBuffer + "}");
            try {
                if (Log.getLogBuffer(logBuffer) == null) {
                    throw new IllegalArgumentException("Invalid log buffer: " + logBuffer);
                }
                List<Handler> logHandlers = new ArrayList<>();
                int priority = arguments.getInt("logPriority", Log.INFO);

                String[] flags = arguments.getStringArray("logFlags");
                boolean consoleLogging = arguments.getBoolean("consoleLogging", false);
                if (consoleLogging) {
                    ConsoleHandler consoleHandler = new ConsoleHandler();
                    if (flags != null && Arrays.asList(flags).contains("timestamp")) {
                        consoleHandler.setFlag(ConsoleHandler.FLAG_TIMESTAMP);
                    }
                    consoleHandler.setPriority(priority);
                    logHandlers.add(consoleHandler);
                } else {
                    Log.println('D', LOG_TAG, "Console logging: disabled");
                }

                boolean fileLogging = arguments.getBoolean("fileLogging", false);
                if (fileLogging) {
                    String directory = arguments.getString("logDirectory", Environment.getLogDirectory().getAbsolutePath());
                    String fileName = arguments.getString("logFileName", "Log" + logBuffer + "-%g.log");
                    int fileLimit = arguments.getInt("logFileLimit", 262144);
                    int fileCount = arguments.getInt("logFileCount", 4);
                    int bufferSize = arguments.getInt("logBufferSize", 0);
                    int dataVolumeLimit = arguments.getInt("logDataVolumeLimit", 0);
                    try {
                        FileHandler fileHandler = new FileHandler(directory + File.separator + fileName, fileLimit, fileCount, true,
                                bufferSize, dataVolumeLimit);
                        fileHandler.setPriority(priority);
                        logHandlers.add(fileHandler);
                    } catch (IOException e) {
                        Log.println('E', LOG_TAG, "File logging error: " + e.getMessage(), e);
                    }
                }

                final String customHandler = arguments.getString("customLogging", null);
                if (customHandler != null && customHandler.length() > 0) {
                    try {
                        Class<?> clazz = Class.forName(customHandler);
                        Handler handler = (Handler) clazz.newInstance();
                        handler.setPriority(priority);
                        logHandlers.add(handler);
                    } catch (Exception e) {
                        Log.println('E', LOG_TAG, "Cannot create custom handler \'" + customHandler + "\': " + e.getMessage(), e);
                    } catch (LinkageError e) {
                        Log.println('E', LOG_TAG, "Linkage error: " + e.getMessage(), e);
                    }
                }

                if (!logHandlers.isEmpty()) {
                    mLogHandlers.put(logBuffer, logHandlers);
                    mLogger.reset();
                    Log.println('D', LOG_TAG, "Logging has been started {" + logBuffer + "}");
                } else {
                    Log.println('D', LOG_TAG, "Logging has been disabled {" + logBuffer + "}");
                }
            } catch (IllegalArgumentException e) {
                Log.println('E', LOG_TAG, "Failed to start logging {" + logBuffer + "}", e);
            }
        }
    }

    private void stopLogging(Bundle arguments) {
        final int logBuffer = arguments.getInt("logBuffer", Log.LOG_ID_MAIN);
        if (mLogHandlers.containsKey(logBuffer)) {
            Log.println('D', LOG_TAG, "Stopping logging {" + logBuffer + "}");
            List<Handler> logHandlers = mLogHandlers.get(logBuffer);
            for (Handler handler : logHandlers) {
                handler.close();
            }
            mLogHandlers.remove(logBuffer);
            Log.println('D', LOG_TAG, "Logging has been stopped {" + logBuffer + "}");
        }
    }

    private void dumpLog(Bundle arguments) {
        final int logBuffer = arguments.getInt("logBuffer", Log.LOG_ID_MAIN);
        final String fileName = arguments.getString("fileName");
        IRemoteCallback callback = IRemoteCallback.Stub.asInterface(arguments.getBinder("binder"));
        if (mLogHandlers.containsKey(logBuffer)) {
            Log.println('D', LOG_TAG, "Dumping log to file " + fileName + " {" + logBuffer + "}");
            List<Handler> logHandlers = mLogHandlers.get(logBuffer);
            Bundle result = new Bundle();
            for (Handler handler : logHandlers) {
                if (handler instanceof FileHandler) {
                    if (((FileHandler) handler).dump(fileName)) {
                        Log.println('D', LOG_TAG, "Log has been dumped to file " + fileName + " {" + logBuffer + "}");
                        result.putBoolean("result", true);
                    } else {
                        Log.println('D', LOG_TAG, "Failed to dump log to file " + fileName + " {" + logBuffer + "}");
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
        }
    }

    private void flushLog(Bundle arguments) {
        final int logBuffer = arguments.getInt("logBuffer", Log.LOG_ID_MAIN);
        if (mLogHandlers.containsKey(logBuffer)) {
            Log.println('D', LOG_TAG, "Flushing log {" + logBuffer + "}");
            List<Handler> logHandlers = mLogHandlers.get(logBuffer);
            for (Handler handler : logHandlers) {
                handler.flush();
            }
        }
    }

    private void clearLog(Bundle arguments) {
        final int logBuffer = arguments.getInt("logBuffer", Log.LOG_ID_MAIN);
        if (mLogHandlers.containsKey(logBuffer)) {
            Log.println('D', LOG_TAG, "Clearing log {" + logBuffer + "}");
            List<Handler> logHandlers = mLogHandlers.get(logBuffer);
            for (Handler handler : logHandlers) {
                if (handler instanceof FileHandler) {
                    ((FileHandler) handler).clear();
                }
            }
        }
    }

    private static class TestHandler extends Handler {
        private List<LogRecord> mLogHistory = new LinkedList<>();
        private List<Assumption> mAssumptions = new LinkedList<>();
        private int mMark = 0;

        private static class Assumption extends Promise<String> {
            private String mTag;
            private String mMessage;

            Assumption(String tag, String message) {
                mTag = tag;
                mMessage = message;
            }

            boolean match(int priority, String tag, String message) {
                if (tag.matches(mTag) && message.matches(mMessage)) {
                    return complete(Logger.LOG_LEVELS[priority] + "/" + tag + ": " + message);
                }
                return false;
            }

            public String toString() {
                return mTag + ": " + mMessage;
            }
        }

        @Override
        public void close() {
        }

        @Override
        public void flush() {
        }

        @Override
        public synchronized void publish(LogRecord logRecord) {
            mLogHistory.add(logRecord);
            Iterator<Assumption> itr = mAssumptions.iterator();
            while (itr.hasNext()) {
                Assumption assumption = itr.next();
                if (assumption.match(logRecord.getPriority(), logRecord.getTag(), logRecord.getMessage())) {
                    itr.remove();
                }
            }
        }

        public synchronized Promise<String> assumeThat(String tag, String message, long timeout) throws RemoteException {
            Assumption assumption = new Assumption(tag, message);
            if (matchLogHistory(assumption)) {
                try {
                    Log.println('D', LOG_TAG, "Log assumption success: " + assumption.get());
                } catch (CancellationException | ExecutionException | InterruptedException ignore) {
                }
                return assumption;
            } else {
                mAssumptions.add(assumption);
                Promise<String> p = assumption.orTimeout(timeout, "Log assumption timeout: " + assumption.toString())
                .catchException(exception -> {
                    synchronized (TestHandler.this) {
                        mAssumptions.remove(assumption);
                    }
                })
                .then((value, exception) -> {
                    if (exception == null) {
                        Log.println('D', LOG_TAG, "Log assumption success: " + value);
                    } else {
                        Log.println('E', LOG_TAG, "Log assumption timeout: " + assumption.toString());
                    }
                });
                return p;
            }
        }

        public synchronized void clear() {
            for (Assumption assumption : mAssumptions) {
                assumption.cancel();
            }
            mAssumptions.clear();
            mLogHistory.clear();
        }

        public synchronized void mark() {
            mMark = mLogHistory.size();
        }

        public synchronized void reset() {
            for (Assumption assumption : mAssumptions) {
                assumption.cancel();
            }
            mAssumptions.clear();
            while (mLogHistory.size() > mMark) {
                mLogHistory.remove(mLogHistory.size() - 1);
            }
        }

        private boolean matchLogHistory(Assumption assumption) {
            for (LogRecord logRecord : mLogHistory) {
                if (logRecord.getTag().matches(assumption.mTag) && logRecord.getMessage().matches(assumption.mMessage)) {
                    return assumption.complete(Logger.LOG_LEVELS[logRecord.getPriority()] + "/" + logRecord.getTag() + ": " + logRecord.getMessage());
                }
            }
            return false;
        }
    }
}
