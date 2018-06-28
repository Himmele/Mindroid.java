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
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import mindroid.util.concurrent.Promise;

public class LogBuffer {
    private static final int TIMESTAMP_SIZE = 8;
    private static final int PRIORITY_SIZE = 4;
    private static final int THREAD_ID_SIZE = 8;
    private static final int TAG_SIZE = 4;
    private static final int MESSAGE_SIZE = 4;

    private final int mId;
    private final int mSize;
    private final byte[] mBuffer;
    private int mReadIndex;
    private int mWriteIndex;
    private final GregorianCalendar mCalendar = new GregorianCalendar();
    private final SimpleDateFormat mFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private Promise<LogRecord> mPromise = null;

    public class LogRecord {
        private long mTimestamp;
        private long mThreadId;
        private int mPriority;
        private String mTag;
        private String mMessage;

        public LogRecord(final long timestamp, final long threadId, final int priority, final String tag, final String message) {
            mTimestamp = timestamp;
            mThreadId = threadId;
            mPriority = priority;
            mTag = tag;
            mMessage = message;
        }

        public String toString() {
            mCalendar.setTimeInMillis(mTimestamp);
            return mFormatter.format(mCalendar.getTime()) + "  " + String.format("%016X", mThreadId) + "  " + Logger.LOG_LEVELS[mPriority] + ' ' + mTag + ": " + mMessage;
        }

        public String toShortString() {
            return Logger.LOG_LEVELS[mPriority] + "/" + mTag + "(" + String.format("%016X", mThreadId) + "): " + mMessage;
        }

        public long getTimestamp() {
            return mTimestamp;
        }

        public long getThreadId() {
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
    }

    public LogBuffer(final int id, final int size) {
        mId = id;
        mSize = size;
        mBuffer = new byte[mSize];
        mReadIndex = 0;
        mWriteIndex = 0;
    }

    public int getId() {
        return mId;
    }

    public synchronized void reset() {
        mReadIndex = 0;
        mWriteIndex = 0;
        if (mPromise != null) {
            mPromise.complete(null);
            mPromise = null;
        }
    }

    public boolean put(final int priority, final String tag, final String message) {
        return put(System.currentTimeMillis(), Thread.currentThread().getId(), priority, tag, message);
    }

    public boolean put(final long timestamp, final long threadId, final int priority, final String tag, final String message) {
        if (tag == null) {
            return false;
        }
        if (message == null) {
            return false;
        }
        byte[] tagBuffer = null;
        byte[] messageBuffer = null;
        try {
            tagBuffer = tag.getBytes("UTF-8");
            messageBuffer = message.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return false;
        }
        final int size = TIMESTAMP_SIZE + THREAD_ID_SIZE + PRIORITY_SIZE + TAG_SIZE + tagBuffer.length + MESSAGE_SIZE + messageBuffer.length;
        if ((size + 4) > mSize) {
            return false;
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(size + 4);
        byteBuffer.putInt(size);
        byteBuffer.putLong(timestamp);
        byteBuffer.putLong(threadId);
        byteBuffer.putInt(priority);
        byteBuffer.putInt(tagBuffer.length);
        byteBuffer.put(tagBuffer);
        byteBuffer.putInt(messageBuffer.length);
        byteBuffer.put(messageBuffer);

        synchronized (this) {
            if (mPromise != null) {
                mPromise.complete(new LogRecord(timestamp, threadId, priority, tag, message));
                mPromise = null;
            } else {
                writeByteArray(byteBuffer.array());
            }
        }

        return true;
    }

    public Promise<LogRecord> get() {
        byte[] buffer;
        synchronized (this) {
            if (mPromise == null) {
                if (!isEmpty()) {
                    final int size = intFromByteArray(readByteArray(4));
                    buffer = readByteArray(size);
                } else {
                    mPromise = new Promise<>();
                    return mPromise;
                }
            } else {
                return mPromise;
            }
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        long timestamp = byteBuffer.getLong();
        long threadId = byteBuffer.getLong();
        int priority = byteBuffer.getInt();
        int tagSize = byteBuffer.getInt();
        byte[] tagData = new byte[tagSize];
        byteBuffer.get(tagData);
        String tag = new String(tagData);
        int messageSize = byteBuffer.getInt();
        byte[] messageData = new byte[messageSize];
        byteBuffer.get(messageData);
        String message = new String(messageData);
        return new Promise<>(new LogRecord(timestamp, threadId, priority, tag, message));
    }

    private boolean isEmpty() {
        return mReadIndex == mWriteIndex;
    }

    @SuppressWarnings("unused")
    private boolean isFull() {
        return (mWriteIndex + 1) % mSize == mReadIndex;
    }

    private int remainingCapacity() {
        if (mWriteIndex >= mReadIndex) {
            return (mSize - (mWriteIndex - mReadIndex));
        } else {
            return (mReadIndex - mWriteIndex);
        }
    }

    private void writeByteArray(final byte[] data) {
        int remainingCapacity = remainingCapacity();
        while (remainingCapacity < data.length) {
            int size = intFromByteArray(readByteArray(4));
            mReadIndex = (mReadIndex + size) % mSize;
            remainingCapacity += (size + 4);
        }

        if (mWriteIndex + data.length < mSize) {
            System.arraycopy(data, 0, mBuffer, mWriteIndex, data.length);
            mWriteIndex = (mWriteIndex + data.length) % mSize;
        } else {
            int partialSize = (mSize - mWriteIndex);
            System.arraycopy(data, 0, mBuffer, mWriteIndex, partialSize);
            System.arraycopy(data, partialSize, mBuffer, 0, data.length - partialSize);
            mWriteIndex = (mWriteIndex + data.length) % mSize;
        }
    }

    private byte[] readByteArray(final int size) {
        byte[] data = new byte[size];
        if (mReadIndex + size < mSize) {
            System.arraycopy(mBuffer, mReadIndex, data, 0, data.length);
            mReadIndex = (mReadIndex + data.length) % mSize;
        } else {
            int partialSize = (mSize - mReadIndex);
            System.arraycopy(mBuffer, mReadIndex, data, 0, partialSize);
            System.arraycopy(mBuffer, 0, data, partialSize, data.length - partialSize);
            mReadIndex = (mReadIndex + data.length) % mSize;
        }
        return data;
    }

    private static int intFromByteArray(final byte[] data) {
        return data[0] << 24 | (data[1] & 0xFF) << 16 | (data[2] & 0xFF) << 8 | (data[3] & 0xFF);
    }
}
