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

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UTFDataFormatException;

/**
 * Wraps an existing {@link InputStream} and reads big-endian typed data from it.
 * Typically, this stream has been written by a DataOutputStream. Types that can
 * be read include byte, 16-bit short, 32-bit int, 32-bit float, 64-bit long,
 * 64-bit double, byte strings, and strings encoded in
 * {@link DataInput modified UTF-8}.
 *
 * @see DataOutputStream
 */
public class DataInputStream extends InputStream implements DataInput {
    /**
     * The source input stream that is filtered.
     */
    protected InputStream mInputStream;

    private final byte[] mScratchpad = new byte[8];

    /**
     * Constructs a new DataInputStream on the InputStream {@code in}. All
     * reads are then filtered through this stream. Note that data read by this
     * stream is not in a human readable format and was most likely created by a
     * DataOutputStream.
     *
     * <p><strong>Warning:</strong> passing a null source creates an invalid
     * {@code DataInputStream}. All operations on such a stream will fail.
     *
     * @param inputStream
     *            the source InputStream the filter reads from.
     * @see DataOutputStream
     * @see RandomAccessFile
     */
    public DataInputStream(InputStream inputStream) {
        mInputStream = inputStream;
    }

    @Override
    public int available() throws IOException {
        return mInputStream.available();
    }

    /**
     * Closes this stream. This implementation closes the filtered stream.
     *
     * @throws IOException
     *             if an error occurs while closing this stream.
     */
    @Override
    public void close() throws IOException {
        mInputStream.close();
    }

    /**
     * Sets a mark position in this stream. The parameter {@code limit}
     * indicates how many bytes can be read before the mark is invalidated.
     * Sending {@code reset()} will reposition this stream back to the marked
     * position, provided that {@code limit} has not been surpassed.
     * <p>
     * This implementation sets a mark in the filtered stream.
     *
     * @param limit
     *            the number of bytes that can be read from this stream before
     *            the mark is invalidated.
     * @see #markSupported()
     * @see #reset()
     */
    @Override
    public void mark(int limit) {
        mInputStream.mark(limit);
    }

    /**
     * Indicates whether this stream supports {@code mark()} and {@code reset()}.
     * This implementation returns whether or not the filtered stream supports
     * marking.
     *
     * @return {@code true} if {@code mark()} and {@code reset()} are supported,
     *         {@code false} otherwise.
     * @see #mark(int)
     * @see #reset()
     * @see #skip(long)
     */
    @Override
    public boolean markSupported() {
        return mInputStream.markSupported();
    }

    /**
     * Reads a single byte from the filtered stream and returns it as an integer
     * in the range from 0 to 255. Returns -1 if the end of this stream has been
     * reached.
     *
     * @return the byte read or -1 if the end of the filtered stream has been
     *         reached.
     * @throws IOException
     *             if the stream is closed or another IOException occurs.
     */
    @Override
    public int read() throws IOException {
        return mInputStream.read();
    }

    @Override
    public final int read(byte[] buffer) throws IOException {
        return mInputStream.read(buffer);
    }

    @Override
    public final int read(byte[] buffer, int offset, int count) throws IOException {
        return mInputStream.read(buffer, offset, count);
    }

    /**
     * Resets this stream to the last marked location. This implementation
     * resets the target stream.
     *
     * @throws IOException
     *             if this stream is already closed, no mark has been set or the
     *             mark is no longer valid because more than {@code limit}
     *             bytes have been read since setting the mark.
     * @see #mark(int)
     * @see #markSupported()
     */
    @Override
    public void reset() throws IOException {
        mInputStream.reset();
    }

    /**
     * Skips {@code byteCount} bytes in this stream. Subsequent
     * calls to {@code read} will not return these bytes unless {@code reset} is
     * used. This implementation skips {@code byteCount} bytes in the
     * filtered stream.
     *
     * @return the number of bytes actually skipped.
     * @throws IOException
     *             if this stream is closed or another IOException occurs.
     * @see #mark(int)
     * @see #reset()
     */
    @Override
    public long skip(long count) throws IOException {
        return mInputStream.skip(count);
    }

    public final boolean readBoolean() throws IOException {
        int value = mInputStream.read();
        if (value < 0) {
            throw new EOFException();
        }
        return value != 0;
    }

    public final byte readByte() throws IOException {
        int value = mInputStream.read();
        if (value < 0) {
            throw new EOFException();
        }
        return (byte) value;
    }

    public final char readChar() throws IOException {
        return (char) readShort();
    }

    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    public final void readFully(byte[] buffer) throws IOException {
        readFully(buffer, 0, buffer.length);
    }

    public final void readFully(byte[] buffer, int offset, int size) throws IOException {
        if (size == 0) {
            return;
        }
        if (buffer == null) {
            throw new NullPointerException("buffer == null");
        }
        if ((offset < 0) || (size < 0) || ((offset + size) > buffer.length)) {
            throw new IndexOutOfBoundsException();
        }
        while (size > 0) {
            int count = mInputStream.read(buffer, offset, size);
            if (count < 0) {
                throw new EOFException();
            }
            offset += count;
            size -= count;
        }
    }

    public final short readShort() throws IOException {
        readFully(mScratchpad, 0, 2);
        return (short) ((mScratchpad[0] << 8) | (mScratchpad[1] & 0xff));
    }

    public final int readInt() throws IOException {
        readFully(mScratchpad, 0, 4);
        return (((mScratchpad[0] & 0xff) << 24) |
                ((mScratchpad[1] & 0xff) << 16) |
                ((mScratchpad[2] & 0xff) <<  8) |
                ((mScratchpad[3] & 0xff) <<  0));
    }

    public final long readLong() throws IOException {
        readFully(mScratchpad, 0, 8);
        return ((((long) (mScratchpad[0] & 0xff)) << 56) |
                (((long) (mScratchpad[1] & 0xff)) << 48) |
                (((long) (mScratchpad[2] & 0xff)) << 40) |
                (((long) (mScratchpad[3] & 0xff)) << 32) |
                (((long) (mScratchpad[4] & 0xff)) << 24) |
                ((mScratchpad[5] & 0xff) << 16) |
                ((mScratchpad[6] & 0xff) <<  8) |
                ((mScratchpad[7] & 0xff) <<  0));
    }

    public final int readUnsignedByte() throws IOException {
        int value = mInputStream.read();
        if (value < 0) {
            throw new EOFException();
        }
        return value;
    }

    public final int readUnsignedShort() throws IOException {
        return ((int) readShort()) & 0xffff;
    }

    public final String readUTF() throws IOException {
        int size = readUnsignedShort();
        byte[] data = new byte[size];
        readFully(data, 0, size);

        char[] string = new char[size];
        int offset = 0;
        int count = 0, s = 0, a;
        while (count < size) {
            if ((string[s] = (char) data[offset + count++]) < '\u0080') {
                s++;
            } else if (((a = string[s]) & 0xe0) == 0xc0) {
                if (count >= size) {
                    throw new UTFDataFormatException("Bad second byte at position " + count);
                }
                int b = data[offset + count++];
                if ((b & 0xC0) != 0x80) {
                    throw new UTFDataFormatException("Bad second byte at position " + (count - 1));
                }
                string[s++] = (char) (((a & 0x1F) << 6) | (b & 0x3F));
            } else if ((a & 0xf0) == 0xe0) {
                if (count + 1 >= size) {
                    throw new UTFDataFormatException("Bad third byte at position " + (count + 1));
                }
                int b = data[offset + count++];
                int c = data[offset + count++];
                if (((b & 0xC0) != 0x80) || ((c & 0xC0) != 0x80)) {
                    throw new UTFDataFormatException("Bad second or third byte at position " + (count - 2));
                }
                string[s++] = (char) (((a & 0x0F) << 12) | ((b & 0x3F) << 6) | (c & 0x3F));
            } else {
                throw new UTFDataFormatException("Bad byte at position " + (count - 1));
            }
        }
        return new String(string, 0, s);
    }

    /**
     * Skips {@code count} number of bytes in this stream. Subsequent {@code
     * read()}s will not return these bytes unless {@code reset()} is used.
     *
     * This method will not throw an {@link EOFException} if the end of the
     * input is reached before {@code count} bytes where skipped.
     *
     * @param count
     *            the number of bytes to skip.
     * @return the number of bytes actually skipped.
     * @throws IOException
     *             if a problem occurs during skipping.
     * @see #mark(int)
     * @see #reset()
     */
    public final int skipBytes(int count) throws IOException {
        int skipped = 0;
        long skip;
        while (skipped < count && (skip = mInputStream.skip(count - skipped)) != 0) {
            skipped += skip;
        }
        return skipped;
    }

    @Override
    public String readLine() throws IOException {
        throw new UnsupportedOperationException();
    }
}
