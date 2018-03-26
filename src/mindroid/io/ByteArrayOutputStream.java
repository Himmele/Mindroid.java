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
import java.io.OutputStream;

/**
 * A specialized {@link OutputStream} for class for writing content to an
 * (internal) byte array. As bytes are written to this stream, the byte array
 * may be expanded to hold more bytes. When the writing is considered to be
 * finished, a copy of the byte array can be requested from the class.
 *
 * @see ByteArrayInputStream
 */
public class ByteArrayOutputStream extends OutputStream {
    /**
     * The byte array containing the bytes written.
     */
    protected byte[] mBuffer;

    /**
     * The number of bytes written.
     */
    protected int mCount;

    /**
     * Constructs a new ByteArrayOutputStream with a default size of 64 bytes.
     * If more than 64 bytes are written to this instance, the underlying byte
     * array will expand.
     */
    public ByteArrayOutputStream() {
        mBuffer = new byte[64];
    }

    /**
     * Constructs a new {@code ByteArrayOutputStream} with a default size of
     * {@code size} bytes. If more than {@code size} bytes are written to this
     * instance, the underlying byte array will expand.
     *
     * @param size
     *            initial size for the underlying byte array, must be
     *            non-negative.
     * @throws IllegalArgumentException
     *             if {@code size} < 0.
     */
    public ByteArrayOutputStream(int size) {
        if (size >= 0) {
            mBuffer = new byte[size];
        } else {
            throw new IllegalArgumentException("size < 0");
        }
    }

    public ByteArrayOutputStream(byte[] buffer) {
        mBuffer = buffer;
    }

    /**
     * Closes this stream. This releases system resources used for this stream.
     *
     * @throws IOException
     *             if an error occurs while attempting to close this stream.
     */
    @Override
    public void close() throws IOException {
        super.close();
    }

    @Override
    public void flush() throws IOException {
    }

    private void expand(int i) {
        if (mCount + i <= mBuffer.length) {
            return;
        }

        byte[] buffer = new byte[(mCount + i) * 2];
        System.arraycopy(mBuffer, 0, buffer, 0, mCount);
        mBuffer = buffer;
    }

    /**
     * Resets this stream to the beginning of the underlying byte array. All
     * subsequent writes will overwrite any bytes previously stored in this
     * stream.
     */
    public void reset() {
        mCount = 0;
    }

    /**
     * Returns the total number of bytes written to this stream so far.
     *
     * @return the number of bytes written to this stream.
     */
    public int size() {
        return mCount;
    }

    /**
     * Returns the contents of this ByteArrayOutputStream as a byte array. Any
     * changes made to the receiver after returning will not be reflected in the
     * byte array returned to the caller.
     *
     * @return this stream's current contents as a byte array.
     */
    public byte[] toByteArray() {
        byte[] byteArray = new byte[mCount];
        System.arraycopy(mBuffer, 0, byteArray, 0, mCount);
        return byteArray;
    }

    public byte[] getByteArray() {
        return mBuffer;
    }

    /**
     * Returns the contents of this ByteArrayOutputStream as a string. Any
     * changes made to the receiver after returning will not be reflected in the
     * string returned to the caller.
     *
     * @return this stream's current contents as a string.
     */
    @Override
    public String toString() {
        return new String(mBuffer, 0, mCount);
    }

    @Override
    public void write(byte[] buffer) {
        write(buffer, 0, buffer.length);
    }

    /**
     * Writes {@code count} bytes from the byte array {@code buffer} starting at
     * offset {@code index} to this stream.
     *
     * @param buffer
     *            the buffer to be written.
     * @param offset
     *            the initial position in {@code buffer} to retrieve bytes.
     * @param count
     *            the number of bytes of {@code buffer} to write.
     * @throws NullPointerException
     *             if {@code buffer} is {@code null}.
     * @throws IndexOutOfBoundsException
     *             if {@code offset < 0} or {@code len < 0}, or if
     *             {@code offset + len} is greater than the length of
     *             {@code buffer}.
     */
    @Override
    public void write(byte[] buffer, int offset, int count) {
        if ((offset < 0) || (count < 0) || ((offset + count) > buffer.length)) {
            throw new IndexOutOfBoundsException();
        }
        if (count == 0) {
            return;
        }
        expand(count);
        System.arraycopy(buffer, offset, mBuffer, mCount, count);
        mCount += count;
    }

    /**
     * Writes the specified byte {@code oneByte} to the OutputStream. Only the
     * low order byte of {@code oneByte} is written.
     *
     * @param b
     *            the byte to be written.
     */
    @Override
    public void write(int b) {
        if (mCount == mBuffer.length) {
            expand(1);
        }
        mBuffer[mCount++] = (byte) b;
    }

    /**
     * Takes the contents of this stream and writes it to the output stream
     * {@code out}.
     *
     * @param outputStream
     *            an OutputStream on which to write the contents of this stream.
     * @throws IOException
     *             if an error occurs while writing to {@code out}.
     */
    public void writeTo(OutputStream outputStream) throws IOException {
        outputStream.write(mBuffer, 0, mCount);
    }
}
