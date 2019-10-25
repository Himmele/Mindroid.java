/*
 * Copyright (C) 2018 E.S.R.Labs
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

package mindroid.runtime.system.aio;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * A specialized {@link OutputStream} for class for writing content to an
 * (internal) byte array. As bytes are written to this stream, the byte array
 * may be expanded to hold more bytes. When the writing is considered to be
 * finished, a copy of the byte array can be requested from the class.
 *
 * @see ByteArrayInputStream
 */
public class SocketOutputStream extends OutputStream {
    protected Socket mSocket;

    /**
     * The {@code ByteBuffer} list containing the bytes to stream over.
     */
    protected Deque<ByteBuffer> mList = new ConcurrentLinkedDeque<>();

    /**
     * Constructs a new ByteArrayOutputStream with a default size of 64 bytes.
     * If more than 64 bytes are written to this instance, the underlying byte
     * array will expand.
     */
    SocketOutputStream(Socket socket) {
        mSocket = socket;
    }

    /**
     * Closes this stream. This releases system resources used for this stream.
     *
     * @throws IOException
     *             if an error occurs while attempting to close this stream.
     */
    @Override
    public void close() throws IOException {
        IOException exception = null;
        try {
            flush();
        } catch (IOException e) {
            exception = e;
        }

        mList.clear();

        if (exception != null) {
            throw exception;
        }
    }

    @Override
    public void flush() throws IOException {
        sync();
    }

    @Override
    public void write(byte[] buffer) throws IOException {
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
    public void write(byte[] buffer, int offset, int count) throws IOException {
        if ((offset < 0) || (count < 0) || ((offset + count) > buffer.length)) {
            throw new IndexOutOfBoundsException();
        }
        if (count == 0) {
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, offset, count);
        mList.add(byteBuffer);
        sync();
    }

    /**
     * Writes the specified byte {@code oneByte} to the OutputStream. Only the
     * low order byte of {@code oneByte} is written.
     *
     * @param b
     *            the byte to be written.
     */
    @Override
    public void write(int b) throws IOException {
        ByteBuffer byteBuffer = (ByteBuffer) ByteBuffer.allocate(1).put((byte) b).flip();
        mList.add(byteBuffer);
        sync();
    }

    void sync() {
        int operation = 0;
        Object arg = null;
        synchronized (this) {
            if (!mList.isEmpty()) {
                ByteBuffer[] buffers = mList.toArray(new ByteBuffer[mList.size()]);
                try {
                    long num = mSocket.write(buffers);
                    if (num > 0) {
                        Iterator<ByteBuffer> itr = mList.iterator();
                        while (itr.hasNext()) {
                            ByteBuffer buffer = itr.next();
                            if (!buffer.hasRemaining()) {
                                itr.remove();
                            } else {
                                break;
                            }
                        }
                        operation = Socket.OP_WRITE;
                    }
                } catch (IOException e) {
                    operation = Socket.OP_CLOSE;
                    arg = e;
                }
            }
        }
        if (operation != 0) {
            mSocket.notifyListener(operation, arg);
        }
    }
}
