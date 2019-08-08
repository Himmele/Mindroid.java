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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A specialized {@link InputStream } for reading the contents of a byte array.
 *
 * @see SocketOutputStream
 */
public class SocketInputStream extends InputStream {
    protected Socket mSocket;

    /**
     * The {@code ByteBuffer} list containing the bytes to stream over.
     */
    protected Deque<ByteBuffer> mList = new ConcurrentLinkedDeque<>();

    /**
     * The total number of bytes initially available in the byte array
     * {@code mBuffer}.
     */
    protected AtomicInteger mCount = new AtomicInteger(0);

    /**
     * Constructs an empty {@code ByteBufferInputStream}.
     */
    SocketInputStream(Socket socket) {
        mSocket = socket;
    }

    /**
     * Returns the number of remaining bytes.
     *
     * @return {@code count - position}
     */
    @Override
    public int available() {
        return mCount.get();
    }

    /**
     * Closes this stream and frees resources associated with this stream.
     *
     * @throws IOException
     *             if an I/O error occurs while closing this stream.
     */
    @Override
    public void close() throws IOException {
        mList.clear();
        mCount.set(0);
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
        return false;
    }

    /**
     * Reads a single byte from the source byte array and returns it as an
     * integer in the range from 0 to 255. Returns -1 if the end of the source
     * array has been reached.
     *
     * @return the byte read or -1 if the end of this stream has been reached.
     */
    @Override
    public int read() throws IOException {
        if (mCount.get() > 0) {
            ByteBuffer headBuffer = mList.getFirst();
            int b = headBuffer.get() & 0xFF;
            mCount.decrementAndGet();
            if (headBuffer.remaining() == 0) {
                mList.removeFirst();
            }
            return b;
        } else {
            throw new IOException("EOS");
        }
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    @Override
    public int read(byte[] buffer, final int offset, final int count) throws IOException {
        if (buffer == null) {
            throw new NullPointerException();
        } else if ((offset < 0) || (count < 0) || ((offset + count) > buffer.length)) {
            throw new IndexOutOfBoundsException();
        }

        if (count > mCount.get()) {
            throw new IOException("EOS");
        }
        if (count == 0) {
            return 0;
        }

        int o = offset;
        int c = count;
        Iterator<ByteBuffer> itr = mList.iterator();
        while (itr.hasNext() && c > 0) {
            ByteBuffer b = itr.next();
            final int remaining = b.remaining();
            if (remaining > 0) {
                final int size = c <= remaining ? c : remaining; 
                System.arraycopy(b.array(), b.position(), buffer, o, size);
                b.position(b.position() + size);
                o += size;
                c -= size;
                mCount.addAndGet(-size);
                if (c == 0 && b.hasRemaining()) {
                    break;
                }
            }
            itr.remove();
        }

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
        long c = count;
        long num = 0;
        Iterator<ByteBuffer> itr = mList.iterator();
        while (itr.hasNext() && c > 0) {
            ByteBuffer b = itr.next();
            final int remaining = b.remaining();
            if (remaining > 0) {
                final int size = (int) (c <= remaining ? c : remaining);
                b.position(b.position() + size);
                num += size;
                c -= size;
                mCount.addAndGet(-size);
                if (c == 0 && b.hasRemaining()) {
                    break;
                }
            }
            itr.remove();
        }
        return num;
    }

    void sync() {
        int operation = 0;
        Object arg = null;
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        synchronized (this) {
            try {
                long num = mSocket.read(buffer);
                if (num == -1) {
                    operation = Socket.OP_CLOSE;
                }
                if (num > 0) {
                    mList.add((ByteBuffer) buffer.flip());
                    mCount.addAndGet(buffer.remaining());
                    operation = Socket.OP_READ;
                }
            } catch (IOException e) {
                operation = Socket.OP_CLOSE;
                arg = e;
            }
        }
        if (operation != 0) {
            mSocket.notifyListener(operation, arg);
        }
    }
}
