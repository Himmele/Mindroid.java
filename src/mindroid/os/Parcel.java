/*
 *  Copyright (C) 2006 The Android Open Source Project
 *  Copyright (C) 2013 Daniel Himmelein
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

package mindroid.os;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.net.URI;
import java.net.URISyntaxException;
import mindroid.runtime.system.Runtime;

public final class Parcel {
    private ByteArrayOutputStream mOutputStream;
    private ByteArrayInputStream mInputStream;
    private DataOutput mDataOutputStream;
    private DataInput mDataInputStream;

    private Parcel() {
        mOutputStream = new ByteArrayOutputStream();
        mDataOutputStream = new DataOutputStream(mOutputStream);
    }

    private Parcel(int size) {
        mOutputStream = new ByteArrayOutputStream(size);
        mDataOutputStream = new DataOutputStream(mOutputStream);
    }

    private Parcel(byte[] buffer, int offset, int size) {
        if (offset == 0 && size == buffer.length) {
            mOutputStream = new ByteArrayOutputStream(buffer);
        } else {
            mOutputStream = new ByteArrayOutputStream(size);
            mOutputStream.write(buffer, offset, size);
        }
        mDataOutputStream = new DataOutputStream(mOutputStream);
        asInput();
    }

    /**
     * Retrieve a new Parcel object from the pool.
     */
    public static Parcel obtain() {
        return new Parcel();
    }

    public static Parcel obtain(int size) {
        return new Parcel(size);
    }

    public static Parcel obtain(byte[] buffer) {
        if (buffer == null) {
            throw new NullPointerException();
        }
        return new Parcel(buffer, 0, buffer.length);
    }

    public static Parcel obtain(byte[] buffer, int offset, int size) {
        if (buffer == null) {
            throw new NullPointerException();
        }
        return new Parcel(buffer, offset, size);
    }

    /**
     * Put a Parcel object back into the pool.  You must not touch
     * the object after this call.
     */
    public final void recycle() {
    }

    /**
     * Returns the total amount of data contained in the parcel.
     */
    public final int size() {
        return mOutputStream.size();
    }

    public final void putBoolean(boolean value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeBoolean(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    public final void putByte(byte value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeByte(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    public final void putChar(char value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeChar(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    public final void putShort(short value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeShort(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Write an integer value into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public final void putInt(int value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeInt(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Write a long integer value into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public final void putLong(long value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeLong(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Write a floating point value into the parcel at the current
     * dataPosition(), growing dataCapacity() if needed.
     */
    public final void putFloat(float value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeFloat(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Write a double precision floating point value into the parcel at the
     * current dataPosition(), growing dataCapacity() if needed.
     */
    public final void putDouble(double value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeDouble(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Write a string value into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public final void putString(String value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeUTF(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    public final void putBytes(byte[] buffer) throws RemoteException {
        checkOutput();
        mOutputStream.write(buffer);
    }

    public final void putBytes(byte[] buffer, int offset, int size) throws RemoteException {
        checkOutput();
        mOutputStream.write(buffer, offset, size);
    }

    public final void putBinder(IBinder binder) throws RemoteException {
        try {
            URI descriptor = new URI(binder.getInterfaceDescriptor());
            URI uri = new URI(binder.getUri().getScheme(), binder.getUri().getAuthority(), "/if=" + descriptor.getPath().substring(1), null, null);
            putString(uri.toString());
        } catch (URISyntaxException e) {
            throw new RemoteException(e);
        }
    }

    public final void putBinder(IBinder base, IBinder binder) throws RemoteException {
        try {
            URI descriptor = new URI(binder.getInterfaceDescriptor());
            URI uri = new URI(base.getUri().getScheme(), binder.getUri().getAuthority(), "/if=" + descriptor.getPath().substring(1), null, null);
            putString(uri.toString());
        } catch (URISyntaxException e) {
            throw new RemoteException(e);
        }
    }

    public final boolean getBoolean() throws RemoteException {
        checkInput();
        try {
            return mDataInputStream.readBoolean();
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    public final byte getByte() throws RemoteException {
        checkInput();
        try {
            return mDataInputStream.readByte();
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    public final char getChar() throws RemoteException {
        checkInput();
        try {
            return mDataInputStream.readChar();
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    public final short getShort() throws RemoteException {
        checkInput();
        try {
            return mDataInputStream.readShort();
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Read an integer value from the parcel at the current dataPosition().
     */
    public final int getInt() throws RemoteException {
        checkInput();
        try {
            return mDataInputStream.readInt();
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Read a long integer value from the parcel at the current dataPosition().
     */
    public final long getLong() throws RemoteException {
        checkInput();
        try {
            return mDataInputStream.readLong();
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Read a floating point value from the parcel at the current
     * dataPosition().
     * @throws IOException 
     */
    public final float getFloat() throws RemoteException {
        checkInput();
        try {
            return mDataInputStream.readFloat();
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Read a double precision floating point value from the parcel at the
     * current dataPosition().
     * @throws IOException 
     */
    public final double getDouble() throws RemoteException {
        checkInput();
        try {
            return mDataInputStream.readDouble();
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Read a string value from the parcel at the current dataPosition().
     * @throws IOException 
     */
    public final String getString() throws RemoteException {
        checkInput();
        try {
            return mDataInputStream.readUTF();
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    public final byte[] getBytes() throws RemoteException {
        checkInput();
        if (mInputStream.available() >= 0) {
            byte[] buffer = new byte[mInputStream.available()];
            mInputStream.read(buffer);
            return buffer;
        } else {
            return null;
        }
    }

    public final byte[] getBytes(int size) throws RemoteException {
        checkInput();
        if (mInputStream.available() >= 0) {
            size = Math.min(mInputStream.available(), size);
            byte[] buffer = new byte[size];
            mInputStream.read(buffer, 0, size);
            return buffer;
        } else {
            return null;
        }
    }

    public final IBinder getBinder() throws RemoteException {
        URI uri;
        try {
            uri = new URI(getString());
        } catch (URISyntaxException e) {
            return null;
        }
        return Runtime.getRuntime().getBinder(uri);
    }

    public final byte[] toByteArray() {
        return mOutputStream.getByteArray();
    }

    public final ByteArrayInputStream asInputStream() {
        if (mInputStream == null) {
            mInputStream = new ByteArrayInputStream(mOutputStream.getByteArray());
            mDataInputStream = new DataInputStream(mInputStream);
        }
        return mInputStream;
    }

    public final ByteArrayOutputStream asOutputStream() {
        if (mInputStream != null) {
            mInputStream = null;
            mDataInputStream = null;
        }
        return mOutputStream;
    }

    public final Parcel asInput() {
        asInputStream();
        return this;
    }

    public final Parcel asOutput() {
        asOutputStream();
        return this;
    }

    private final void checkOutput() {
        if (mInputStream != null) {
            throw new IllegalStateException("Parcel is in input mode");
        }
    }

    private final void checkInput() {
        if (mInputStream == null) {
            throw new IllegalStateException("Parcel is in output mode");
        }
    }

    public static final URI toUri(IBinder base, IBinder binder) throws RemoteException {
        try {
            URI descriptor = new URI(binder.getInterfaceDescriptor());
            URI uri = new URI(base.getUri().getScheme(), binder.getUri().getAuthority(), "/if=" + descriptor.getPath().substring(1), null, null);
            return uri;
        } catch (URISyntaxException e) {
            throw new RemoteException(e);
        }
    }

    public static final IBinder fromUri(URI uri) throws RemoteException {
        return Runtime.getRuntime().getBinder(uri);
    }

    public static class ByteArrayOutputStream extends OutputStream {
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

    public static class ByteArrayInputStream extends InputStream {
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

    private static class DataOutputStream extends OutputStream implements DataOutput {
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

    private static class DataInputStream extends InputStream implements DataInput {
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
}
