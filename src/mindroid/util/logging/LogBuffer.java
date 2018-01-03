/*
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

package mindroid.util.logging;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

public class LogBuffer {
    private static final int TIMESTAMP_SIZE = 8;
    private static final int PRIO_SIZE = 4;
    private static final int THREAD_ID_SIZE = 4;
    private static final int TAG_SIZE = 4;
    private static final int MESSAGE_SIZE = 4;
    private static final String[] sLogLevels = { "V", "D", "I", "W", "E", "A" };

    private final int ID;
    private final int SIZE;
    private int mReadIndex;
    private int mWriteIndex;
    private byte[] mData;
    private final GregorianCalendar mCalendar = new GregorianCalendar();
    private final SimpleDateFormat mFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private boolean mQuitting = false;

    public class LogRecord {
        private int mLogId;
        private long mTimestamp;
        private int mThreadId;
        private int mPriority;
        private String mTag;
        private String mMessage;

        public LogRecord(final int logId, final long timestamp, final int threadId, final int priority, final String tag, final String message) {
            mLogId = logId;
            mTimestamp = timestamp;
            mThreadId = threadId;
            mPriority = priority;
            mTag = tag;
            mMessage = message;
        }

        public String toShortString() {
            return sLogLevels[mPriority] + "/" + mTag + "(" + toHexString(mThreadId) + "): " + mMessage;
        }

        public String toString() {
            mCalendar.setTimeInMillis(mTimestamp);
            return mFormatter.format(mCalendar.getTime()) + "  " + toHexString(mThreadId) + "  " + sLogLevels[mPriority] + ' ' + mTag + ": " + mMessage;
        }

        public int getLogId() {
            return mLogId;
        }

        public long getTimestamp() {
            return mTimestamp;
        }

        public int getThreadId() {
            return mThreadId;
        }

        public int getPriority() {
            return mPriority;
        }

        public String getTag() {
            return mTag;
        }

        public String getMessage() {
            return mMessage;
        }

        private String toHexString(int value) {
            String hexString = Integer.toHexString(value);
            switch (hexString.length()) {
            case 1:
                return "0000000" + hexString;
            case 2:
                return "000000" + hexString;
            case 3:
                return "00000" + hexString;
            case 4:
                return "0000" + hexString;
            case 5:
                return "000" + hexString;
            case 6:
                return "00" + hexString;
            case 7:
                return "0" + hexString;
            case 8:
                return hexString;
            case 0:
            default:
                return "00000000";
            }
        }
    }

    public LogBuffer(final int id, final int size) {
        ID = id;
        SIZE = size;
        mData = new byte[SIZE];
        reset();
    }

    public boolean offer(final int priority, final String tag, final String message) {
        return offer(System.currentTimeMillis(), Thread.currentThread().hashCode(), priority, tag, message);
    }

    public boolean offer(final long timestamp, final int threadId, final int priority, final String tag, final String message) {
        if (tag == null) {
            return false;
        }
        if (message == null) {
            return false;
        }
        byte[] tagData = null;
        byte[] messageData = null;
        try {
            tagData = tag.getBytes("UTF-8");
            messageData = message.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return false;
        }
        int size = TIMESTAMP_SIZE + THREAD_ID_SIZE + PRIO_SIZE + TAG_SIZE + tagData.length + MESSAGE_SIZE + messageData.length;
        if ((size + 4) > SIZE) {
            return false;
        }

        byte[] logMessageSize = new byte[4];
        intToByteArray(size, logMessageSize);
        byte[] logMessage = new byte[size];
        longToByteArray(timestamp, logMessage);
        intToByteArray(threadId, logMessage, TIMESTAMP_SIZE);
        intToByteArray(priority, logMessage, TIMESTAMP_SIZE + THREAD_ID_SIZE);
        intToByteArray(tagData.length, logMessage, TIMESTAMP_SIZE + THREAD_ID_SIZE + PRIO_SIZE);
        System.arraycopy(tagData, 0, logMessage, TIMESTAMP_SIZE + THREAD_ID_SIZE + PRIO_SIZE + TAG_SIZE, tagData.length);
        intToByteArray(messageData.length, logMessage, TIMESTAMP_SIZE + THREAD_ID_SIZE + PRIO_SIZE + TAG_SIZE + tagData.length);
        System.arraycopy(messageData, 0, logMessage, TIMESTAMP_SIZE + THREAD_ID_SIZE + PRIO_SIZE + TAG_SIZE + tagData.length + MESSAGE_SIZE, messageData.length);

        synchronized (this) {
            free(logMessageSize.length + logMessage.length);
            write(logMessageSize);
            write(logMessage);
            notify();
        }

        return true;
    }

    public LogRecord take(final int minPriority) throws InterruptedException {
        while (true) {
            byte[] record;
            synchronized (this) {
                while (isEmpty()) {
                    wait();
                    if (mQuitting) {
                        mQuitting = false;
                        return null;
                    }
                }
                int size = intFromByteArray(read(4));
                record = read(size);
            }
            long timestamp = longFromByteArray(record);
            int threadId = intFromByteArray(record, TIMESTAMP_SIZE);
            int priority = intFromByteArray(record, TIMESTAMP_SIZE + THREAD_ID_SIZE);
            int tagSize = intFromByteArray(record, TIMESTAMP_SIZE + THREAD_ID_SIZE + PRIO_SIZE);
            String tag = new String(record, TIMESTAMP_SIZE + THREAD_ID_SIZE + PRIO_SIZE + TAG_SIZE, tagSize);
            int messageSize = intFromByteArray(record, TIMESTAMP_SIZE + THREAD_ID_SIZE + PRIO_SIZE + TAG_SIZE + tagSize);
            String message = new String(record, TIMESTAMP_SIZE + THREAD_ID_SIZE + PRIO_SIZE + TAG_SIZE + tagSize + MESSAGE_SIZE, messageSize);
            if (priority >= minPriority) {
                return new LogRecord(ID, timestamp, threadId, priority, tag, message);
            }
        }
    }

    public LogRecord poll(final int minPriority) {
        while (true) {
            byte[] record;
            synchronized (this) {
                while (isEmpty()) {
                    return null;
                }
                int size = intFromByteArray(read(4));
                record = read(size);
            }
            long timestamp = longFromByteArray(record);
            int threadId = intFromByteArray(record, TIMESTAMP_SIZE);
            int priority = intFromByteArray(record, TIMESTAMP_SIZE + THREAD_ID_SIZE);
            int tagSize = intFromByteArray(record, TIMESTAMP_SIZE + THREAD_ID_SIZE + PRIO_SIZE);
            String tag = new String(record, TIMESTAMP_SIZE + THREAD_ID_SIZE + PRIO_SIZE + TAG_SIZE, tagSize);
            int messageSize = intFromByteArray(record, TIMESTAMP_SIZE + THREAD_ID_SIZE + PRIO_SIZE + TAG_SIZE + tagSize);
            String message = new String(record, TIMESTAMP_SIZE + THREAD_ID_SIZE + PRIO_SIZE + TAG_SIZE + tagSize + MESSAGE_SIZE, messageSize);
            if (priority >= minPriority) {
                return new LogRecord(ID, timestamp, threadId, priority, tag, message);
            }
        }
    }

    public synchronized void quit() {
        mQuitting = true;
        notify();
    }

    public synchronized boolean isEmpty() {
        return mReadIndex == mWriteIndex;
    }

    public synchronized boolean isFull() {
        return (mWriteIndex + 1) % SIZE == mReadIndex;
    }

    private int remainingCapacity() {
        if (mWriteIndex >= mReadIndex) {
            return (SIZE - (mWriteIndex - mReadIndex));
        } else {
            return (mReadIndex - mWriteIndex);
        }
    }

    private void free(final int size) {
        int remainingCapacity = remainingCapacity();
        while (remainingCapacity < size) {
            int curSize = intFromByteArray(read(4));
            mReadIndex = (mReadIndex + curSize) % SIZE;
            remainingCapacity += (curSize + 4);
        }
    }

    public synchronized void reset() {
        mReadIndex = 0;
        mWriteIndex = 0;
    }

    public int getId() {
        return ID;
    }

    private void write(final byte[] data) {
        if (mWriteIndex + data.length < SIZE) {
            System.arraycopy(data, 0, mData, mWriteIndex, data.length);
            mWriteIndex = (mWriteIndex + data.length) % SIZE;
        } else {
            int partialSize = (SIZE - mWriteIndex);
            System.arraycopy(data, 0, mData, mWriteIndex, partialSize);
            System.arraycopy(data, partialSize, mData, 0, data.length - partialSize);
            mWriteIndex = (mWriteIndex + data.length) % SIZE;
        }
    }

    private byte[] read(final int size) {
        byte[] data = new byte[size];
        if (mReadIndex + size < SIZE) {
            System.arraycopy(mData, mReadIndex, data, 0, data.length);
            mReadIndex = (mReadIndex + data.length) % SIZE;
        } else {
            int partialSize = (SIZE - mReadIndex);
            System.arraycopy(mData, mReadIndex, data, 0, partialSize);
            System.arraycopy(mData, 0, data, partialSize, data.length - partialSize);
            mReadIndex = (mReadIndex + data.length) % SIZE;
        }
        return data;
    }

    private static void intToByteArray(final int i, final byte[] dest) {
        dest[0] = (byte) (i >> 24);
        dest[1] = (byte) (i >> 16);
        dest[2] = (byte) (i >> 8);
        dest[3] = (byte) (i);
    }

    private static void intToByteArray(final int i, final byte[] dest, final int destPos) {
        dest[destPos + 0] = (byte) (i >> 24);
        dest[destPos + 1] = (byte) (i >> 16);
        dest[destPos + 2] = (byte) (i >> 8);
        dest[destPos + 3] = (byte) (i);
    }

    private static void longToByteArray(final long l, final byte[] dest) {
        dest[0] = (byte) (l >> 56);
        dest[1] = (byte) (l >> 48);
        dest[2] = (byte) (l >> 40);
        dest[3] = (byte) (l >> 32);
        dest[4] = (byte) (l >> 24);
        dest[5] = (byte) (l >> 16);
        dest[6] = (byte) (l >> 8);
        dest[7] = (byte) (l);
    }

    private static int intFromByteArray(final byte[] src) {
        return src[0] << 24 | (src[1] & 0xFF) << 16 | (src[2] & 0xFF) << 8 | (src[3] & 0xFF);
    }

    private static int intFromByteArray(final byte[] src, final int srcPos) {
        return src[srcPos + 0] << 24 | (src[srcPos + 1] & 0xFF) << 16 | (src[srcPos + 2] & 0xFF) << 8 | (src[srcPos + 3] & 0xFF);
    }

    private static long longFromByteArray(final byte[] src) {
        return ((long) src[0]) << 56 | ((long) (src[1] & 0xFF)) << 48 | ((long) (src[2] & 0xFF)) << 40 | ((long) (src[3] & 0xFF)) << 32
                | ((long) (src[4] & 0xFF)) << 24 | ((long) (src[5] & 0xFF)) << 16 | ((long) (src[6] & 0xFF)) << 8 | ((long) (src[7] & 0xFF));
    }
}
