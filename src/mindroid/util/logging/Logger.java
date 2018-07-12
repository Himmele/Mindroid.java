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

import mindroid.content.Context;
import mindroid.os.RemoteException;
import mindroid.os.ServiceManager;
import mindroid.util.concurrent.ExecutionException;
import mindroid.util.concurrent.Promise;

public class Logger {
    public static final String ACTION_LOG = "mindroid.util.logging.LOG";
    public static final String ACTION_DUMP_LOG = "mindroid.util.logging.DUMP_LOG";
    public static final String ACTION_FLUSH_LOG = "mindroid.util.logging.FLUSH_LOG";
    public static final String ACTION_CLEAR_LOG = "mindroid.util.logging.CLEAR_LOG";
    public static final String ACTION_MARK_LOG = "mindroid.util.logging.MARK_LOG";
    public static final String ACTION_RESET_LOG = "mindroid.util.logging.RESET_LOG";

    /** @hide */
    public static final String[] LOG_LEVELS = { "V", "D", "I", "W", "E", "A" };

    private ILogger mLogger;

    /**
     * @hide
     */
    public Logger() {
        mLogger = ILogger.Stub.asInterface(ServiceManager.getSystemService(Context.LOGGER_SERVICE));
    }

    public Logger(Context context) {
        mLogger = ILogger.Stub.asInterface(context.getSystemService(Context.LOGGER_SERVICE));
    }

    public Promise<String> assumeThat(String tag, String message, long timeout) {
        if ((mLogger != null) && (tag != null) && (message != null) && (timeout >= 0)) {
            try {
                return mLogger.assumeThat(tag, message, timeout);
            } catch (RemoteException e) {
                throw new RuntimeException("System failure", e);
            }
        } else {
            return new Promise<>(new ExecutionException());
        }
    }
}
