/*
 *  Copyright (C) 2018 Daniel Himmelein
 * 
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package mindroid.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * A specialized {@link InputStream } for reading the contents of a byte array.
 *
 * @see ByteArrayOutputStream
 */
public class ByteArrayInputStream extends InputStream {
    /**
     * The {@code byte} array containing the bytes to stream over.
     */
    protected byte[] mBuffer;

    /**
     * The current position within the byte array.
     */
    protected int mPosition;

    /**
     * The current mark position. Initially set to 0 or the <code>offset</code>
     * parameter within the constructor.
     */
    protected int mMark;

    /**
     * The total number of bytes initially available in the byte array
     * {@code mBuffer}.
     */
    protected int mCount;

    /**
     * Constructs a new {@code ByteArrayInputStream} on the byte array
     * {@code buf}.
     *
     * @param buf
     *            the byte array to stream over.
     */
    public ByteArrayInputStream(byte[] buffer) {
        mBuffer = buffer;
        mPosition = 0;
        mMark = 0;
        mCount = buffer.length;
    }

    /**
     * Constructs a new {@code ByteArrayInputStream} on the byte array
     * {@code buf} with the initial position set to {@code offset} and the
     * number of bytes available set to {@code offset} + {@code length}.
     *
     * @param buffer
     *            the byte array to stream over.
     * @param offset
     *            the initial position in {@code buf} to start streaming from.
     * @param count
     *            the number of bytes available for streaming.
     */
    public ByteArrayInputStream(byte[] buffer, int offset, int count) {
        mBuffer = buffer;
        mPosition = offset;
        mMark = offset;
        mCount = offset + count > buffer.length ? buffer.length : offset + count;
    }

    /**
     * Returns the number of remaining bytes.
     *
     * @return {@code count - position}
     */
    @Override
    public int available() {
        return mCount - mPosition;
    }

    /**
     * Closes this stream and frees resources associated with this stream.
     *
     * @throws IOException
     *             if an I/O error occurs while closing this stream.
     */
    @Override
    public void close() throws IOException {
    }

    /**
     * Sets a mark position in this ByteArrayInputStream. The parameter
     * {@code limit} is ignored. Sending {@code reset()} will reposition the
     * stream back to the marked position.
     *
     * @param limit
     *            ignored.
     * @see #markSupported()
     * @see #reset()
     */
    @Override
    public void mark(int limit) {
        mMark = mPosition;
    }

    /**
     * Indicates whether this stream supports the {@code mark()} and
     * {@code reset()} methods. Returns {@code true} since this class supports
     * these methods.
     *
     * @return always {@code true}.
     * @see #mark(int)
     * @see #reset()
     */
    @Override
    public boolean markSupported() {
        return true;
    }

    /**
     * Reads a single byte from the source byte array and returns it as an
     * integer in the range from 0 to 255. Returns -1 if the end of the source
     * array has been reached.
     *
     * @return the byte read or -1 if the end of this stream has been reached.
     */
    @Override
    public int read() {
        return mPosition < mCount ? mBuffer[mPosition++] & 0xFF : -1;
    }

    @Override
    public int read(byte[] buffer) {
        return read(buffer, 0, buffer.length);
    }

    @Override
    public int read(byte[] buffer, int offset, int count) {
        if (buffer == null) {
            throw new NullPointerException();
        } else if ((offset < 0) || (count < 0) || ((offset + count) > buffer.length)) {
            throw new IndexOutOfBoundsException();
        }

        if (mPosition >= mCount) {
            return -1;
        }
        if (count == 0) {
            return 0;
        }

        count = mCount - mPosition < count ? mCount - mPosition : count;
        System.arraycopy(mBuffer, mPosition, buffer, offset, count);
        mPosition += count;
        return count;
    }

    /**
     * Resets this stream to the last marked location. This implementation
     * resets the position to either the marked position, the start position
     * supplied in the constructor or 0 if neither has been provided.
     *
     * @see #mark(int)
     */
    @Override
    public void reset() {
        mPosition = mMark;
    }

    /**
     * Skips {@code byteCount} bytes in this InputStream. Subsequent
     * calls to {@code read} will not return these bytes unless {@code reset} is
     * used. This implementation skips {@code byteCount} number of bytes in the
     * target stream. It does nothing and returns 0 if {@code byteCount} is negative.
     *
     * @return the number of bytes actually skipped.
     */
    @Override
    public long skip(long count) {
        if (count <= 0) {
            return 0;
        }
        int position = mPosition;
        mPosition = mCount - mPosition < count ? mCount : (int) (mPosition + count);
        return mPosition - position;
    }
}
