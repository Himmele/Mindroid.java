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
import java.net.StandardSocketOptions;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.CompletableFuture;

public class ServerSocket implements SelectableSocket {
    private final ServerSocketChannel mServerSocketChannel;
    private Selector mSelector;
    private Listener mListener;
    private int mOps = 0;

    public static final int OP_CLOSE = 1;
    public static final int OP_ACCEPT = 2;

    public static interface Listener {
        public abstract void onOperation(int operation, Object arg);
    }

    public ServerSocket(SocketAddress socketAddress) throws IOException {
        mServerSocketChannel = ServerSocketChannel.open();
        try {
            mServerSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            mServerSocketChannel.bind(socketAddress);
            mServerSocketChannel.configureBlocking(false);
        } catch (IOException | RuntimeException e) {
            try {
                mServerSocketChannel.close();
            } catch (IOException ignore) {
            }
            throw e;
        }
        mOps = SelectionKey.OP_ACCEPT;
    }

    @Override
    public void close() throws IOException {
        mOps = 0;
        mServerSocketChannel.close();
        if (mSelector != null) {
            mSelector.wakeup();
        }
    }

    public CompletableFuture<Socket> accept() {
        CompletableFuture<Socket> future = new CompletableFuture<>();
        try {
            future.complete(new Socket(mServerSocketChannel.accept()));
        } catch (IOException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    public SocketAddress getLocalAddress() throws IOException {
        return mServerSocketChannel.getLocalAddress();
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public boolean isOpen(){
        return mServerSocketChannel.isOpen();
    }

    @Override
    public SelectionKey register(Selector selector) throws ClosedChannelException {
        mSelector = selector;
        return mServerSocketChannel.register(selector, mOps);
    }

    @Override
    public void onOperation(int ops) {
        if ((ops & SelectionKey.OP_ACCEPT) != 0) {
            if (mListener != null) {
                mListener.onOperation(ServerSocket.OP_ACCEPT, null);
            }
        }
    }
}
