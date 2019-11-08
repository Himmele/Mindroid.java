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
import java.util.concurrent.atomic.AtomicInteger;

public class SocketOutputStream extends OutputStream {
    protected static final int MAX_BUFFER_SIZE = 8192;

    protected final Socket mSocket;

    /**
     * The {@code ByteBuffer} list containing the bytes to stream over.
     */
    protected final Deque<ByteBuffer> mBuffer = new ConcurrentLinkedDeque<>();

    /**
     * The total number of bytes initially available in the byte array
     * {@code mBuffer}.
     */
    protected final AtomicInteger mCount = new AtomicInteger(0);

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

        mBuffer.clear();

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
        mBuffer.add(byteBuffer);
        if (mCount.addAndGet(count) >= MAX_BUFFER_SIZE) {
            sync();
        }
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
        mBuffer.add(byteBuffer);
        if (mCount.incrementAndGet() >= MAX_BUFFER_SIZE) {
            sync();
        }
    }

    void sync() {
        int operation = 0;
        Object arg = null;
        synchronized (this) {
            if (!mBuffer.isEmpty()) {
                ByteBuffer[] buffers = mBuffer.toArray(new ByteBuffer[0]);
                try {
                    long num = mSocket.write(buffers);
                    if (num > 0) {
                        mCount.addAndGet((int) -num);
                        Iterator<ByteBuffer> itr = mBuffer.iterator();
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
