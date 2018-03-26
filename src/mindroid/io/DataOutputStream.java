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

import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UTFDataFormatException;

/**
 * Wraps an existing {@link OutputStream} and writes big-endian typed data to it.
 * Typically, this stream can be read in by DataInputStream. Types that can be
 * written include byte, 16-bit short, 32-bit int, 32-bit float, 64-bit long,
 * 64-bit double, byte strings, and {@link DataInput MUTF-8} encoded strings.
 *
 * @see DataInputStream
 */
public class DataOutputStream extends OutputStream implements DataOutput {
    /**
     * The target output stream for this filter stream.
     */
    protected OutputStream mOutputStream;

    private final byte[] mScratchpad = new byte[8];

    /**
     * Constructs a new {@code DataOutputStream} on the {@code OutputStream}
     * {@code out}. Note that data written by this stream is not in a human
     * readable form but can be reconstructed by using a {@link DataInputStream}
     * on the resulting output.
     *
     * @param outputStream
     *            the target stream for writing.
     */
    public DataOutputStream(OutputStream outputStream) {
        mOutputStream = outputStream;
    }

    /**
     * Closes this stream. This implementation closes the target stream.
     *
     * @throws IOException
     *             if an error occurs attempting to close this stream.
     */
    @Override
    public void close() throws IOException {
        IOException exception = null;
        try {
            flush();
        } catch (IOException e) {
            exception = e;
        }

        try {
            mOutputStream.close();
        } catch (IOException e) {
            if (exception == null) {
                exception = e;
            }
        }

        if (exception != null) {
            throw exception;
        }
    }

    /**
     * Flushes this stream to ensure all pending data is sent out to the target
     * stream. This implementation then also flushes the target stream.
     *
     * @throws IOException
     *             if an error occurs attempting to flush this stream.
     */
    @Override
    public void flush() throws IOException {
        mOutputStream.flush();
    }

    /**
     * Equivalent to {@code write(buffer, 0, buffer.length)}.
     */
    @Override
    public void write(byte[] buffer) throws IOException {
        write(buffer, 0, buffer.length);
    }

    /**
     * Writes {@code count} bytes from the byte array {@code buffer} starting at
     * {@code offset} to the target stream.
     *
     * @param buffer
     *            the buffer to write to the target stream.
     * @param offset
     *            the index of the first byte in {@code buffer} to write.
     * @param count
     *            the number of bytes from the {@code buffer} to write.
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @throws NullPointerException
     *             if {@code buffer} is {@code null}.
     */
    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        if (buffer == null) {
            throw new NullPointerException("buffer == null");
        }
        mOutputStream.write(buffer, offset, count);
    }

    /**
     * Writes a byte to the target stream. Only the least significant byte of
     * the integer {@code oneByte} is written.
     *
     * @param b
     *            the byte to write to the target stream.
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @see DataInputStream#readByte()
     */
    @Override
    public void write(int b) throws IOException {
        mOutputStream.write(b);
    }

    /**
     * Writes a boolean to the target stream.
     *
     * @param value
     *            the boolean value to write to the target stream.
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @see DataInputStream#readBoolean()
     */
    public final void writeBoolean(boolean value) throws IOException {
        mOutputStream.write(value ? 1 : 0);
    }

    /**
     * Writes an 8-bit byte to the target stream. Only the least significant
     * byte of the integer {@code val} is written.
     *
     * @param value
     *            the byte value to write to the target stream.
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @see DataInputStream#readByte()
     * @see DataInputStream#readUnsignedByte()
     */
    public final void writeByte(int value) throws IOException {
        mOutputStream.write(value);
    }

    public final void writeBytes(String string) throws IOException {
        if (string.length() == 0) {
            return;
        }
        byte[] data = new byte[string.length()];
        for (int i = 0; i < string.length(); i++) {
            data[i] = (byte) string.charAt(i);
        }
        mOutputStream.write(data);
    }

    public final void writeChar(int value) throws IOException {
        writeShort(value);
    }

    public final void writeChars(String string) throws IOException {
        byte[] data = string.getBytes("UTF-16BE");
        mOutputStream.write(data);
    }

    public final void writeDouble(double value) throws IOException {
        writeLong(Double.doubleToLongBits(value));
    }

    public final void writeFloat(float value) throws IOException {
        writeInt(Float.floatToIntBits(value));
    }

    public final void writeShort(int value) throws IOException {
        mScratchpad[0] = (byte) ((value >>  8) & 0xff);
        mScratchpad[1] = (byte) ((value >>  0) & 0xff);
        mOutputStream.write(mScratchpad, 0, 2);
    }

    public final void writeInt(int value) throws IOException {
        mScratchpad[0] = (byte) ((value >> 24) & 0xff);
        mScratchpad[1] = (byte) ((value >> 16) & 0xff);
        mScratchpad[2] = (byte) ((value >>  8) & 0xff);
        mScratchpad[3] = (byte) ((value >>  0) & 0xff);
        mOutputStream.write(mScratchpad, 0, 4);
    }

    public final void writeLong(long value) throws IOException {
        mScratchpad[0] = (byte) ((value >> 56) & 0xff);
        mScratchpad[1] = (byte) ((value >> 48) & 0xff);
        mScratchpad[2] = (byte) ((value >> 40) & 0xff);
        mScratchpad[3] = (byte) ((value >> 32) & 0xff);
        mScratchpad[4] = (byte) ((value >> 24) & 0xff);
        mScratchpad[5] = (byte) ((value >> 16) & 0xff);
        mScratchpad[6] = (byte) ((value >>  8) & 0xff);
        mScratchpad[7] = (byte) ((value >>  0) & 0xff);
        mOutputStream.write(mScratchpad, 0, 8);
    }

    public final void writeUTF(String string) throws IOException {
        int count = 0;
        final int size = string.length();
        for (int i = 0; i < size; ++i) {
            char c = string.charAt(i);
            if (c != 0 && c <= 127) { // 2 byte U+0000.
                ++count;
            } else if (c <= 2047) {
                count += 2;
            } else {
                count += 3;
            }
            if (count > 65535) {
                throw new UTFDataFormatException("String too long (> 65535 B)");
            }
        }

        byte[] data = new byte[2 + count];
        data[0] = (byte) ((count >> 8) & 0xff);
        data[1] = (byte) ((count >> 0) & 0xff);

        int offset = 2;
        for (int i = 0; i < size; ++i) {
            char c = string.charAt(i);
            if (c != 0 && c <= 127) { // 2 byte U+0000.
                data[offset++] = (byte) c;
            } else if (c <= 2047) {
                data[offset++] = (byte) (0xc0 | (0x1f & (c >> 6)));
                data[offset++] = (byte) (0x80 | (0x3f & c));
            } else {
                data[offset++] = (byte) (0xe0 | (0x0f & (c >> 12)));
                data[offset++] = (byte) (0x80 | (0x3f & (c >> 6)));
                data[offset++] = (byte) (0x80 | (0x3f & c));
            }
        }

        write(data);
    }
}
