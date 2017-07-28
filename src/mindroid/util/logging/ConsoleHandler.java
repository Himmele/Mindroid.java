/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mindroid.util.logging;

import mindroid.util.logging.LogBuffer.LogRecord;

/**
 * A handler that writes log messages to the standard output stream {@code System.out}.
 */
public class ConsoleHandler extends Handler {
    public static final int FLAG_TIMESTAMP = 1;
    private static final int MAX_SYSOUT_LINE_LENGTH = 255;

    private int mFlags = 0;

    /**
     * Constructs a {@code ConsoleHandler} object.
     */
    public ConsoleHandler() {
    }

    /**
     * Flushes and closes all opened files.
     */
    public void close() {
    }

    public void flush() {
    }

    /**
     * Publish a {@code LogRecord}.
     * 
     * @param record The log record.
     */
    public void publish(LogRecord record) {
        String output;
        if ((mFlags & FLAG_TIMESTAMP) == FLAG_TIMESTAMP) {
            output = record.toString();
        } else {
            output = record.toShortString();
        }

        for (int i = 0; i < output.length(); i += MAX_SYSOUT_LINE_LENGTH) {
            String o = output.substring(i, Math.min(output.length(), i + MAX_SYSOUT_LINE_LENGTH));
            System.out.print(o);
            System.out.flush();
        }
        System.out.println();
    }

    public void setFlag(int flag) {
        mFlags |= flag;
    }

    public void removeFlag(int flag) {
        mFlags &= ~flag;
    }
}
