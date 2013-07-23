/*
 * Copyright (C) 2006 The Android Open Source Project
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

package mindroid.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;

/**
 * Mindroid logger.
 *
 * <p>Generally, use the Log.v() Log.d() Log.i() Log.w() and Log.e()
 * methods.
 *
 * <p>The order in terms of verbosity, from least to most is
 * ERROR, WARN, INFO, DEBUG, VERBOSE.  Verbose should never be compiled
 * into an application except during development.  Debug logs are compiled
 * in but stripped at runtime.  Error, warning and info logs are always kept.
 *
 * <p><b>Tip:</b> A good convention is to declare a <code>LOG_TAG</code> constant
 * in your class:
 *
 * <pre>private static final String LOG_TAG = "MyService";</pre>
 *
 * and use that in subsequent calls to the log methods.
 * </p>
 *
 * <p><b>Tip:</b> Don't forget that when you make a call like
 * <pre>Log.v(LOG_TAG, "index=" + i);</pre>
 * that when you're building the string to pass into Log.d, the compiler uses a
 * StringBuilder and at least three allocations occur: the StringBuilder
 * itself, the buffer, and the String object.  Realistically, there is also
 * another buffer allocation and copy, and even more pressure on the gc.
 * That means that if your log message is filtered out, you might be doing
 * significant work and incurring significant overhead.
 */
public final class Log {
    public static final int VERBOSE = 0;
    public static final int DEBUG = 1;
    public static final int INFO = 2;
    public static final int WARN = 3;
    public static final int ERROR = 4;
    public static final int WTF = 5;

    private Log() {
    }

    /**
     * Send a {@link #VERBOSE} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int v(String tag, String msg) {
        return println(LOG_ID_MAIN, VERBOSE, tag, msg);
    }

    /**
     * Send a {@link #VERBOSE} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int v(String tag, String msg, Throwable tr) {
    	return println(LOG_ID_MAIN, VERBOSE, tag, msg + '\n' + getStackTraceString(tr));
    }

    /**
     * Send a {@link #DEBUG} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int d(String tag, String msg) {
    	return println(LOG_ID_MAIN, DEBUG, tag, msg);
    }

    /**
     * Send a {@link #DEBUG} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int d(String tag, String msg, Throwable tr) {
    	return println(LOG_ID_MAIN, DEBUG, tag, msg + '\n' + getStackTraceString(tr));
    }

    /**
     * Send an {@link #INFO} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int i(String tag, String msg) {
    	return println(LOG_ID_MAIN, INFO, tag, msg);
    }

    /**
     * Send a {@link #INFO} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int i(String tag, String msg, Throwable tr) {
    	return println(LOG_ID_MAIN, INFO, tag, msg + '\n' + getStackTraceString(tr));
    }

    /**
     * Send a {@link #WARN} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int w(String tag, String msg) {
    	return println(LOG_ID_MAIN, WARN, tag, msg);
    }

    /**
     * Send a {@link #WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int w(String tag, String msg, Throwable tr) {
    	return println(LOG_ID_MAIN, WARN, tag, msg + '\n' + getStackTraceString(tr));
    }    

    /*
     * Send a {@link #WARN} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param tr An exception to log
     */
    public static int w(String tag, Throwable tr) {
    	return println(LOG_ID_MAIN, WARN, tag, getStackTraceString(tr));
    }

    /**
     * Send an {@link #ERROR} log message.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int e(String tag, String msg) {
    	return println(LOG_ID_MAIN, ERROR, tag, msg);
    }

    /**
     * Send a {@link #ERROR} log message and log the exception.
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr An exception to log
     */
    public static int e(String tag, String msg, Throwable tr) {
    	return println(LOG_ID_MAIN, ERROR, tag, msg + '\n' + getStackTraceString(tr));
    }

    /**
     * What a Terrible Failure: Report a condition that should never happen.
     * The error will always be logged at level ASSERT with the call stack.
     * Depending on system configuration, a report may be added to the
     * {@link android.os.DropBoxManager} and/or the process may be terminated
     * immediately with an error dialog.
     * @param tag Used to identify the source of a log message.
     * @param msg The message you would like logged.
     */
    public static int wtf(String tag, String msg) {
    	return println(LOG_ID_MAIN, WTF, tag, msg);
    }

    /**
     * What a Terrible Failure: Report an exception that should never happen.
     * Similar to {@link #wtf(String, String)}, with an exception to log.
     * @param tag Used to identify the source of a log message.
     * @param tr An exception to log.
     */
    public static int wtf(String tag, Throwable tr) {
    	return println(LOG_ID_MAIN, WTF, tag, getStackTraceString(tr));
    }
    
    public static int wtf(String tag, String msg, Throwable tr) {
    	return println(LOG_ID_MAIN, WTF, tag, msg + '\n' + getStackTraceString(tr));
    }

    /**
     * Handy function to get a loggable stack trace from a Throwable
     * @param tr An exception to log
     */
    public static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }

        // This is to reduce the amount of log spew that apps do in the non-error
        // condition of the network being unavailable.
        Throwable t = tr;
        while (t != null) {
            if (t instanceof UnknownHostException) {
                return "";
            }
            t = t.getCause();
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        return sw.toString();
    }
    
    /** @hide */
    public static LogBuffer getLogBuffer(int logId) {
    	switch (logId) {
    	case LOG_ID_MAIN:
    		return sMainLogBuffer;
    	case LOG_ID_TASK_MANAGER:
    		return sTaskManagerLogBuffer;
    	default:
    		return null;
    	}
    }
    
    /** @hide */
    public static void reset(int logId) {
    	switch (logId) {
    	case LOG_ID_MAIN:
    		sMainLogBuffer.reset();
    		break;
    	case LOG_ID_TASK_MANAGER:
    		sTaskManagerLogBuffer.reset();
    		break;
    	}
    }

	public static Integer parsePriority(String priority) {
		char c;
		if (priority.length() > 1 && priority.toUpperCase().equals("WTF")) {
			c = 'A';
		} else {
			c = priority.charAt(0);
		}

		switch (c) {
			case 'V': return new Integer(Log.VERBOSE);
			case 'D': return new Integer(Log.DEBUG);
			case 'I': return new Integer(Log.INFO);
			case 'W': return new Integer(Log.WARN);
			case 'E': return new Integer(Log.ERROR);
			case 'A': return new Integer(Log.WTF);
			default: return null;
		}
	}
	
	public static String toPriority(int priority) {
		String[] logLevels = { "V", "D", "I", "W", "E", "A" };
		if (priority >= 0 && priority < logLevels.length) {
			return logLevels[priority];
		} else {
			return null;
		}
	}
	
	/**
     * Low-level logging call.
     * @param priority The priority/type of this log message
     * @param tag Used to identify the source of a log message.  It usually identifies
     *        the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @return The number of bytes written.
     */
    public static int println(int priority, String tag, String msg) {
    	return println(LOG_ID_MAIN, priority, tag, msg);
    }
    
    /** @hide */
    public static int println(int logId, int priority, String tag, String msg) {
    	switch (logId) {
    	case LOG_ID_MAIN:
    		sMainLogBuffer.enqueue(priority, tag, msg);
    		return 0;
    	case LOG_ID_TASK_MANAGER:
    		sTaskManagerLogBuffer.enqueue(priority, tag, msg);
    		return 0;
    	default:
    		return -1;
    	}
    }
    
    /** @hide */
    public static int println(int logId, long timestamp, int threadId, int priority, String tag, String msg) {
    	switch (logId) {
    	case LOG_ID_MAIN:
    		sMainLogBuffer.enqueue(timestamp, threadId, priority, tag, msg);
    		return 0;
    	case LOG_ID_TASK_MANAGER:
    		sTaskManagerLogBuffer.enqueue(timestamp, threadId, priority, tag, msg);
    		return 0;
    	default:
    		return -1;
    	}
    }
    
    /** @hide */ public static final int LOG_ID_MAIN = 0;
    /** @hide */ public static final int LOG_ID_TASK_MANAGER = 1;
    
    private static final int SIZE_256_KB = 262144;
	private static LogBuffer sMainLogBuffer = new LogBuffer(LOG_ID_MAIN, SIZE_256_KB);
	private static LogBuffer sTaskManagerLogBuffer = new LogBuffer(LOG_ID_TASK_MANAGER, SIZE_256_KB);
}
