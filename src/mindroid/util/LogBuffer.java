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

package mindroid.util;

import java.io.UnsupportedEncodingException;

public class LogBuffer {
	private static final int TIMESTAMP_SIZE = 8;
	private static final int PRIO_SIZE = 4;
	private static final int THREAD_ID_SIZE = 4;
	private static final int TAG_SIZE = 4;
	private static final int MESSAGE_SIZE = 4;
	private String[] mLogLevels = { "V", "D", "I", "W", "E", "A" };
	
	private final int ID;
	private final int SIZE;
	private int mReadIndex;
	private int mWriteIndex;
	private byte[] mArray;
	private byte[] mIntByteArray;
	private byte[] mLongByteArray;
	private GregorianCalendar mCalendar = new GregorianCalendar();
	private boolean mQuit = false;

	public class LogMessage {
		private int mLogId;
		private long mTimestamp;
		private int mThreadId;
		private int mPriority;
		private String mTag;
		private String mMessage;

		public LogMessage(int logId, long timestamp, int threadId, int priority, String tag, String message) {
			mLogId = logId;
			mTimestamp = timestamp;
			mThreadId = threadId;
			mPriority = priority;
			mTag = tag;
			mMessage = message;
		}
		
		public String toString() {
			return mLogLevels[mPriority] + "/" + mTag + "(" + toHexString(mThreadId) + "): " + mMessage;
		}
		
		public String toString(boolean timestamp) {
			if (timestamp) {
				mCalendar.setTimeInMillis(mTimestamp);
				return mCalendar.toString() + "  " + toHexString(mThreadId) + "  " +  mLogLevels[mPriority] + ' ' + mTag + ": " + mMessage;
			} else {
				return toString();
			}
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
			String hexString = Integer.toHexString(mThreadId);
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
	
	public LogBuffer(int id, int size) {
		ID = id;
        SIZE = size;
        mArray = new byte[SIZE];              
        mIntByteArray = new byte[4];
        mLongByteArray = new byte[8];
        reset();
	}
	
	public boolean enqueue(int priority, String tag, String message) {
		return enqueue(System.currentTimeMillis(), Thread.currentThread().hashCode(), priority, tag, message);
	}
	
	public synchronized boolean enqueue(long timestamp, int threadId, int priority, String tag, String message) {
		if (tag == null) {
			tag = "";
		}
		if (message == null) {
			message = "";
		}
		
		byte[] tagData = null;
		byte[] messageData = null;
		try {
			tagData = tag.getBytes("UTF-8");
			messageData = message.getBytes("UTF-8");
        } catch(UnsupportedEncodingException e) {
        	return false;
        }
		
		int size = 4 + TIMESTAMP_SIZE + THREAD_ID_SIZE + PRIO_SIZE + TAG_SIZE + tagData.length + MESSAGE_SIZE + messageData.length;
		if (size >= SIZE) {
			return false;
		}
		
		free(size);
        write(intToByteArray(size));
        write(longToByteArray(timestamp));
        write(intToByteArray(threadId));
        write(intToByteArray(priority));
    	write(intToByteArray(tagData.length));
		write(tagData);
    	write(intToByteArray(messageData.length));
		write(messageData);
        notify();
        return true;
	}
	
	public synchronized LogMessage dequeue(int minPriority) throws InterruptedException {
	    while (true) {
            while (isEmpty()) {
        		wait();
        		if (mQuit) {
        			mQuit = false;
        			return null;
        		}
            }
            int size = intFromByteArray(read(4));
            long timestamp = longFromByteArray(read(8));
            int threadId = intFromByteArray(read(4));
            int priority = intFromByteArray(read(4));
            int tagSize = intFromByteArray(read(4));
            String tag = new String(read(tagSize));
            int messageSize = intFromByteArray(read(4));
            String message = new String(read(messageSize));
            if (priority >= minPriority) {
            	return new LogMessage(ID, timestamp, threadId, priority, tag, message);
            }
	    }
	}
	
	public synchronized void resume() {
		mQuit = true;
		notify();
	}
	
	private boolean isEmpty() {
		return mReadIndex == mWriteIndex;
	}
	
	private boolean isFull() {
		return (mWriteIndex + 1) % SIZE == mReadIndex;
	}
	
	private int remainingCapacity() {
		if (mWriteIndex >= mReadIndex) {
			return (SIZE - (mWriteIndex - mReadIndex));
		} else {
			return (mReadIndex - mWriteIndex);
		}
	}
	
	private void free(int size) {
		int remainingCapacity = remainingCapacity();
		while (remainingCapacity < size) {
			int curSize = intFromByteArray(read(4));
			mReadIndex = (mReadIndex + (curSize - 4)) % SIZE;
			remainingCapacity += curSize;
		}
	}
	
	synchronized void reset() {
		mReadIndex = 0;
        mWriteIndex = 0;
	}
	
	private void write(byte[] data) {
		if (mWriteIndex + data.length < SIZE) {
			System.arraycopy(data, 0, mArray, mWriteIndex, data.length);
	        mWriteIndex = (mWriteIndex + data.length) % SIZE;
		} else {
			int partialSize = (SIZE - mWriteIndex);
			System.arraycopy(data, 0, mArray, mWriteIndex, partialSize);
			System.arraycopy(data, partialSize, mArray, 0, data.length - partialSize);
			mWriteIndex = (mWriteIndex + data.length) % SIZE;
		}
	}
	
	private byte[] read(int size) {
		byte[] data = new byte[size];
		if (mReadIndex + size < SIZE) {
			System.arraycopy(mArray, mReadIndex, data, 0, data.length);
			mReadIndex = (mReadIndex + data.length) % SIZE;
		} else {
			int partialSize = (SIZE - mReadIndex);
			System.arraycopy(mArray, mReadIndex, data, 0, partialSize);
			System.arraycopy(mArray, 0, data, partialSize, data.length - partialSize);
			mReadIndex = (mReadIndex + data.length) % SIZE;
		}
		return data;
	}
	
	private byte[] intToByteArray(int i) {
		mIntByteArray[0] = (byte) (i >> 24);
		mIntByteArray[1] = (byte) (i >> 16);
		mIntByteArray[2] = (byte) (i >> 8);
		mIntByteArray[3] = (byte) (i);
		return mIntByteArray;
	}
	
	private byte[] longToByteArray(long l) {
		mLongByteArray[0] = (byte) (l >> 56);
		mLongByteArray[1] = (byte) (l >> 48);
		mLongByteArray[2] = (byte) (l >> 40);
		mLongByteArray[3] = (byte) (l >> 32);
		mLongByteArray[4] = (byte) (l >> 24);
		mLongByteArray[5] = (byte) (l >> 16);
		mLongByteArray[6] = (byte) (l >> 8);
		mLongByteArray[7] = (byte) (l);
		return mLongByteArray;
	}
	
	private int intFromByteArray(byte[] data) {
	     return data[0] << 24 | (data[1] & 0xFF) << 16 | (data[2] & 0xFF) << 8 | (data[3] & 0xFF);
	}
	
	private long longFromByteArray(byte[] data) {
	     return ((long) data[0]) << 56 | ((long) (data[1] & 0xFF)) << 48 | ((long) (data[2] & 0xFF)) << 40 | ((long) (data[3] & 0xFF)) << 32 |
	    		 ((long) (data[4] & 0xFF)) << 24 | ((long) (data[5] & 0xFF)) << 16 | ((long) (data[6] & 0xFF)) << 8 | ((long) (data[7] & 0xFF));
	}
}
