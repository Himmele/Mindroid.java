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
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;

import mindroid.util.Log;
import mindroid.util.concurrent.Promise;

public class Socket {
    private static final String LOG_TAG = "Socket";
    private static final int CONNECTION_ESTABLISHMENT_TIMEOUT = 10_000;
    private final SocketChannel mSocketChannel;
    private final SocketInputStream mInputStream;
    private final SocketOutputStream mOutputStream;
    private Selector mSelector;
    private CompletableFuture<Void> mConnector;
    private Listener mListener;
    private int mOps = 0;

    public static final int OP_CLOSE = 1;
    public static final int OP_READ = 2;
    public static final int OP_WRITE = 4;

    public static interface Listener {
        public abstract void onOperation(int operation, Object arg);
    }

    public Socket() throws IOException {
        this(SocketChannel.open());
        mOps = 0;
    }

    Socket(SocketChannel socketChannel) throws IOException {
        mSocketChannel = socketChannel;
        mSocketChannel.configureBlocking(false);
        mInputStream = new SocketInputStream(this);
        mOutputStream = new SocketOutputStream(this);
        mOps = SelectionKey.OP_READ;
    }

    public void close() throws IOException {
        mOps = 0;
        mSocketChannel.close();
        if (mSelector != null) {
            mSelector.wakeup();
        }
    }

    public void bind(SocketAddress socketAddress) throws IOException {
        mSocketChannel.bind(socketAddress);
    }

    public CompletableFuture<Void> connect(SocketAddress socketAddress) {
        mOps |= SelectionKey.OP_CONNECT;
        mConnector = new CompletableFuture<>();
        CompletableFuture<Void> future = mConnector.whenComplete((value, exception) -> {
            if (exception == null) {
                mOps = SelectionKey.OP_READ;
                mSelector.wakeup();
                mOutputStream.sync();
            }
        });
        new Promise<Void>(future).orTimeout(CONNECTION_ESTABLISHMENT_TIMEOUT)
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
            if ((mOps & SelectionKey.OP_WRITE) == 1) {
                mOps &= ~SelectionKey.OP_WRITE;
                mSelector.wakeup();
            }
        } else {
            if ((mOps & SelectionKey.OP_WRITE) == 0) {
                mOps |= SelectionKey.OP_WRITE;
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
            if ((mOps & SelectionKey.OP_WRITE) == 1) {
                mOps &= ~SelectionKey.OP_WRITE;
                mSelector.wakeup();
            }
        } else {
            if ((mOps & SelectionKey.OP_WRITE) == 0) {
                mOps |= SelectionKey.OP_WRITE;
                mSelector.wakeup();
            }
        }
        return num;
    }

    public SocketInputStream getInputStream() {
        return mInputStream;
    }

    public SocketOutputStream getOutputStream() {
        return mOutputStream;
    }

    public SocketAddress getLocalAddress() throws IOException {
        return mSocketChannel.getLocalAddress();
    }

    public SocketAddress getRemoteAddress() throws IOException {
        return mSocketChannel.getRemoteAddress();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    SocketChannel getChannel() {
        return mSocketChannel;
    }

    void setSelector(Selector selector) {
        mSelector = selector;
    }

    int getOps() {
        return mOps;
    }

    void onOperation(int ops) {
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
