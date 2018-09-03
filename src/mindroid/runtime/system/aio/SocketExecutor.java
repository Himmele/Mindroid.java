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
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class SocketExecutor {
    private final Executor mExecutor;
    private final Selector mSelector;
    private final Map<SelectableChannel, ServerSocket> mServerSockets = new ConcurrentHashMap<>();
    private final Map<SelectableChannel, Socket> mSockets = new ConcurrentHashMap<>();
    private volatile boolean mShutdown = false;

    public SocketExecutor(Executor executor) throws IOException {
        mExecutor = executor;
        mSelector = Selector.open();
        mExecutor.execute(this::run);
    }

    public void shutdown() {
        mShutdown = true;
        mSelector.wakeup();
    }

    void register(ServerSocket serverSocket) {
        mServerSockets.put(serverSocket.getChannel(), serverSocket);
        serverSocket.setSelector(mSelector);
        mSelector.wakeup();
    }

    void unregister(ServerSocket serverSocket) {
        mServerSockets.remove(serverSocket.getChannel(), serverSocket);
        mSelector.wakeup();
    }

    void register(Socket socket) {
        mSockets.put(socket.getChannel(), socket);
        socket.setSelector(mSelector);
        mSelector.wakeup();
    }

    void unregister(Socket socket) {
        mSockets.remove(socket.getChannel(), socket);
        mSelector.wakeup();
    }

    protected void run() {
        while (!Thread.currentThread().isInterrupted() && !mShutdown) {
            try {
                Iterator<Map.Entry<SelectableChannel, Socket>> socketIterator = mSockets.entrySet().iterator();
                while (socketIterator.hasNext()) {
                    Map.Entry<SelectableChannel, Socket> entry = socketIterator.next();
                    SelectableChannel channel = entry.getKey();
                    Socket socket = entry.getValue();
                    if (channel.isOpen()) {
                        channel.register(mSelector, socket.getOps());
                    }
                }
                Iterator<Map.Entry<SelectableChannel, ServerSocket>> serverSocketIterator = mServerSockets.entrySet().iterator();
                while (serverSocketIterator.hasNext()) {
                    Map.Entry<SelectableChannel, ServerSocket> entry = serverSocketIterator.next();
                    SelectableChannel channel = entry.getKey();
                    ServerSocket serverSocket = entry.getValue();
                    if (channel.isOpen()) {
                        channel.register(mSelector, serverSocket.getOps());
                    }
                }
                mSelector.select();
            } catch (IOException e) {
                break;
            }

            Set<SelectionKey> keys = mSelector.selectedKeys();
            Iterator<SelectionKey> itr = keys.iterator();

            while (itr.hasNext()) {
                SelectionKey key = itr.next();
                itr.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isValid() && key.isAcceptable()) {
                    ServerSocket serverSocket = mServerSockets.get((ServerSocketChannel) key.channel());
                    serverSocket.onOperation(SelectionKey.OP_ACCEPT);
                }
                if (key.isValid() && key.isConnectable()) {
                    Socket socket = mSockets.get((SocketChannel) key.channel());
                    if (socket != null) {
                        socket.onOperation(SelectionKey.OP_CONNECT);
                    }
                }
                if (key.isValid() && key.isReadable()) {
                    Socket socket = mSockets.get((SocketChannel) key.channel());
                    if (socket != null) {
                        socket.onOperation(SelectionKey.OP_READ);
                    }
                }
                if (key.isValid() && key.isWritable()) {
                    Socket socket = mSockets.get((SocketChannel) key.channel());
                    if (socket != null) {
                        socket.onOperation(SelectionKey.OP_WRITE);
                    }
                }
            }
        }

        for (Socket socket : mSockets.values()) {
            try {
                socket.close();
            } catch (IOException ignore) {
            }
        }
        mSockets.clear();

        for (ServerSocket serverSocket : mServerSockets.values()) {
            try {
                serverSocket.close();
            } catch (IOException ignore) {
            }
        }
        mServerSockets.clear();
    }
}
