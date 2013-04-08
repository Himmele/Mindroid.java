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

package mindroid.util;

import java.util.GregorianCalendar;

public class CircularLogBuffer {
	private static final int TIMESTAMP_SIZE = 8;
	private static final int PRIO_SIZE = 4;
	private String[] mLogLevels = { "V", "D", "I", "W", "E", "A" };
	
	private final int SIZE;
	private int mReadIndex;
	private int mWriteIndex;
	private byte[] mArray;
	byte[] mIntByteArray;
	byte[] mLongByteArray;
	GregorianCalendar mCalendar = new GregorianCalendar();

	public class LogMessage {
		private long mTimestamp;
		private int mPriority;
		private String mTag;
		private String mMessage;

		public LogMessage(long timestamp, int priority, String tag, String message) {
			mTimestamp = timestamp;
			mPriority = priority;
			mTag = tag;
			mMessage = message;
		}
		
		public String toString() {
			return mLogLevels[mPriority] + ' ' + mTag + ": " + mMessage;
		}
		
		public String toString(boolean timestamp) {
			if (timestamp) {
				mCalendar.setTimeInMillis(mTimestamp);
				return mCalendar.toString() + " " + mLogLevels[mPriority] + ' ' + mTag + ": " + mMessage;
			} else {
				return toString();
			}
		}
		
		public long getTimestamp() {
			return mTimestamp;
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
	
	public CircularLogBuffer(int size) {
        SIZE = size;
        mArray = new byte[SIZE];              
        mIntByteArray = new byte[4];
        mLongByteArray = new byte[8];
        reset();
	}
	
	public synchronized boolean insertLogMessage(int priority, String tag, String message) {
		int size = 4 + TIMESTAMP_SIZE + PRIO_SIZE + tag.length() + message.length();
		if ((size + 4) >= SIZE) {
			return false;
		}
        while (!hasFreeSpace(size + 4)) {
        	deleteLogMessage();
        }
        
        write(intToByteArray(size));
        write(longToByteArray(System.currentTimeMillis()));
        write(intToByteArray(tag.length()));
        write(intToByteArray(priority));
		write(tag.getBytes());
        write(message.getBytes());
	    
        notify();
        return true;
	}	
	
	public synchronized LogMessage getLogMessage(int minPriority) {
	    while (true) {
			try {
	            while (isEmpty()) {
	                wait();
	            }
	            int size = intFromByteArray(read(4));
	            long timestamp = longFromByteArray(read(8));
	            int tagSize = intFromByteArray(read(4));
	            int priority = intFromByteArray(read(4));
	            String tag = new String(read(tagSize));
	            String msg = new String(read(size - TIMESTAMP_SIZE - tagSize - PRIO_SIZE - 4));
	            if (priority >= minPriority) {
	            	return new LogMessage(timestamp, priority, tag, msg);
	            }
		    } catch (InterruptedException e) {
	            return null;
		    }
	    }
	}
	
	boolean isEmpty() {
        return mReadIndex == mWriteIndex;               
	}
	
	boolean isFull() {
        return (mWriteIndex + 1) % SIZE == mReadIndex;
	}
	
	boolean hasFreeSpace(int size) {
		if (mWriteIndex >= mReadIndex) {
			return (SIZE - (mWriteIndex - mReadIndex) > size);
		} else {
			return (mReadIndex - mWriteIndex > size);
		}
	}
	
	void deleteLogMessage() {
		int size = intFromByteArray(read(4));
        mReadIndex = (mReadIndex + size) % SIZE;
	}
	
	void reset() {
		mReadIndex = 0;
        mWriteIndex = 0;
	}
	
	void write(byte[] data) {
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
	
	byte[] read(int size) {
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
	
	byte[] intToByteArray(int i) {
		mIntByteArray[0] = (byte) (i >> 24);
		mIntByteArray[1] = (byte) (i >> 16);
		mIntByteArray[2] = (byte) (i >> 8);
		mIntByteArray[3] = (byte) (i);
		return mIntByteArray;
	}
	
	byte[] longToByteArray(long l) {
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
	
	int intFromByteArray(byte[] data) {
	     return data[0] << 24 | (data[1] & 0xFF) << 16 | (data[2] & 0xFF) << 8 | (data[3] & 0xFF);
	}
	
	long longFromByteArray(byte[] data) {
	     return ((long) data[0]) << 56 | ((long) (data[1] & 0xFF)) << 48 | ((long) (data[2] & 0xFF)) << 40 | ((long) (data[3] & 0xFF)) << 32 |
	    		 ((long) (data[4] & 0xFF)) << 24 | ((long) (data[5] & 0xFF)) << 16 | ((long) (data[6] & 0xFF)) << 8 | ((long) (data[7] & 0xFF));
	}	
}
