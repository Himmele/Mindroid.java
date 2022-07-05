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

import mindroid.util.Log;
import mindroid.util.concurrent.Executors;
import mindroid.util.concurrent.Promise;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class Socket implements SelectableSocket {
    private static final String LOG_TAG = "Socket";
    private static final int CONNECTION_ESTABLISHMENT_TIMEOUT = 10_000;
    private final SocketChannel mSocketChannel;
    private final CompletableFuture<Void> mConnectionFuture = new CompletableFuture<>();
    private Selector mSelector;
    private CompletableFuture<Void> mConnector;
    private Listener mListener;
    private AtomicInteger mOps = new AtomicInteger(0);
    protected final SocketInputStream mInputStream;
    protected final SocketOutputStream mOutputStream;

    public static final int OP_CLOSE = 1;
    public static final int OP_READ = 2;
    public static final int OP_WRITE = 4;

    public static interface Listener {
        public abstract void onOperation(int operation, Object arg);
    }

    public Socket() throws IOException {
        this(SocketChannel.open());
        mOps.set(0);
    }

    protected Socket(SocketChannel socketChannel) throws IOException {
        mSocketChannel = socketChannel;
        mSocketChannel.configureBlocking(false);
        mInputStream = new SocketInputStream(this);
        mOutputStream = new SocketOutputStream(this);
        mOps.set(SelectionKey.OP_READ);
    }

    public boolean isClosed() {
        return !mSocketChannel.isOpen();
    }

    public boolean isConnected() {
        return mSocketChannel.isConnected();
    }

    public void shutdownInput() throws IOException {
        mSocketChannel.shutdownInput();
    }

    public void shutdownOutput() throws IOException {
        mSocketChannel.shutdownOutput();
    }

    @Override
    public void close() throws IOException {
        mOps.set(0);
        mSocketChannel.close();
        if (mSelector != null) {
            mSelector.wakeup();
        }
    }

    public void bind(SocketAddress socketAddress) throws IOException {
        mSocketChannel.bind(socketAddress);
    }

    public CompletableFuture<Void> connect(SocketAddress socketAddress) {
        mOps.getAndUpdate(value -> value | SelectionKey.OP_CONNECT);
        mConnector = new CompletableFuture<>();
        CompletableFuture<Void> future = mConnector.whenComplete((value, exception) -> {
            if (exception == null) {
                mOps.set(SelectionKey.OP_READ);
                mSelector.wakeup();
                mOutputStream.sync();
            }
        });
        Promise<Void> timeout = new Promise<>(Executors.SYNCHRONOUS_EXECUTOR);
        timeout.completeWith(future);
        timeout.orTimeout(CONNECTION_ESTABLISHMENT_TIMEOUT)
                .catchException(ex -> {
                    try {
                        close();
                        mConnector.completeExceptionally(new SocketTimeoutException("Failed to connect to " + socketAddress));
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Failed to close socket", e);
                    }
                });
        try {
            mSocketChannel.connect(socketAddress);
        } catch (IOException e) {
            mConnector.completeExceptionally(e);
        }
        return future;
    }

    int read(ByteBuffer buffer) throws IOException {
        if (!mSocketChannel.isConnected()) {
            return 0;
        }

        int num = mSocketChannel.read(buffer);
        return num;
    }

    long read(ByteBuffer[] buffers) throws IOException {
        if (!mSocketChannel.isConnected()) {
            return 0;
        }

        long num = mSocketChannel.read(buffers);
        return num;
    }

    int write(ByteBuffer buffer) throws IOException {
        if (!mSocketChannel.isConnected()) {
            return 0;
        }

        int num = mSocketChannel.write(buffer);
        if (!buffer.hasRemaining()) {
            int prevOps = mOps.getAndUpdate(value -> value & ~SelectionKey.OP_WRITE);
            if ((prevOps & SelectionKey.OP_WRITE) != 0) {
                mSelector.wakeup();
            }
        } else {
            int prevOps = mOps.getAndUpdate(value -> value | SelectionKey.OP_WRITE);
            if ((prevOps & SelectionKey.OP_WRITE) == 0) {
                mSelector.wakeup();
            }
        }
        return num;
    }

    long write(ByteBuffer[] buffers) throws IOException {
        if (!mSocketChannel.isConnected()) {
            return 0;
        }

        long num = mSocketChannel.write(buffers);
        if (!buffers[buffers.length - 1].hasRemaining()) {
            int prevValue = mOps.getAndUpdate(value -> value & ~SelectionKey.OP_WRITE);
            if ((prevValue & SelectionKey.OP_WRITE) != 0) {
                mSelector.wakeup();
            }
        } else {
            int prevValue = mOps.getAndUpdate(value -> value | SelectionKey.OP_WRITE);
            if ((prevValue & SelectionKey.OP_WRITE) == 0) {
                mSelector.wakeup();
            }
        }
        return num;
    }

    public InputStream getInputStream() {
        return mInputStream;
    }

    public OutputStream getOutputStream() {
        return mOutputStream;
    }

    public SocketAddress getLocalAddress() throws IOException {
        return mSocketChannel.getLocalAddress();
    }

    public SocketAddress getRemoteAddress() throws IOException {
        return mSocketChannel.getRemoteAddress();
    }

    public <T> Socket setOption(SocketOption<T> name, T value) throws IOException {
        mSocketChannel.setOption(name, value);
        return this;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    protected CompletableFuture<Void> awaitConnection() {
        return mConnectionFuture;
    }

    @Override
    public boolean isOpen() {
        return mSocketChannel.isOpen();
    }

    @Override
    public SelectionKey register(Selector selector) throws ClosedChannelException {
        // Ensure that mConnectionFuture is completed by the same thread that performs OP_READ/OP_WRITE operations (and before the first OP_READ operation).
        mConnectionFuture.complete(null);
        mSelector = selector;
        return mSocketChannel.register(selector, mOps.get());
    }

    @Override
    public void onOperation(int ops) {
        if ((ops & SelectionKey.OP_CONNECT) != 0) {
            if (mConnector != null) {
                try {
                    mSocketChannel.finishConnect();
                    mConnector.complete(null);
                } catch (IOException e) {
                    mConnector.completeExceptionally(e);
                }
                mConnector = null;
            }
        }
        if ((ops & SelectionKey.OP_READ) != 0 && mSocketChannel.isConnected()) {
            mInputStream.sync();
        }
        if ((ops & SelectionKey.OP_WRITE) != 0 && mSocketChannel.isConnected()) {
            mOutputStream.sync();
        }
    }

    void notifyListener(int operation, Object arg) {
        if (mListener != null) {
            mListener.onOperation(operation, arg);
        }
    }
}
