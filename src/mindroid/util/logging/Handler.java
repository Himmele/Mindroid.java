/*
 * Copyright (C) 2017 Daniel Himmelein
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

import mindroid.util.Log;
import mindroid.util.logging.LogBuffer.LogRecord;

public abstract class Handler {
    private int mPriority = Log.INFO;

    public int getPriority() {
        return mPriority;
    }

    public void setPriority(int priority) {
        mPriority = priority;
    }

    /**
     * Closes this handler. A flush operation will be performed and all the
     * associated resources will be freed. Client applications should not use
     * this handler after closing it.
     */
    public abstract void close();

    /**
     * Flushes any buffered output.
     */
    public abstract void flush();

    /**
     * Accepts a logging request and sends it to the the target.
     *
     * @param record
     *            the log record to be logged; {@code null} records are ignored.
     */
    public abstract void publish(LogRecord record);
}
