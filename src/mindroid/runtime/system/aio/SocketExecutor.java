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
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import mindroid.util.Log;

public class SocketExecutor {
    private static final String LOG_TAG = "SocketExecutor";
    private final Executor mExecutor;
    private final Selector mSelector;
    private final Set<SelectableSocket> mSockets = ConcurrentHashMap.newKeySet();

    public SocketExecutor(Executor executor) throws IOException {
        mExecutor = executor;
        mSelector = Selector.open();
        mExecutor.execute(this::run);
    }

    public void shutdown() {
        try {
            mSelector.close();
        } catch (IOException ignore) {
        }
    }

    public void register(SelectableSocket socket) {
        mSockets.add(socket);
        mSelector.wakeup();
    }

    public void unregister(SelectableSocket socket) {
        if (mSockets.remove(socket)) {
            mSelector.wakeup();
        }
    }

    protected void run() {
        while (!Thread.currentThread().isInterrupted() && mSelector.isOpen()) {
            Set<SelectionKey> keys;
            try {
                Iterator<SelectableSocket> socketIterator = mSockets.iterator();
                while (socketIterator.hasNext()) {
                    SelectableSocket socket = socketIterator.next();
                    if (socket.isOpen()) {
                        try {
                            socket.register(mSelector).attach(socket);
                        } catch (CancelledKeyException | ClosedChannelException ignore) {
                        }
                    }
                }
                mSelector.select();
                keys = mSelector.selectedKeys();
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                break;
            } catch (ClosedSelectorException e) {
                break;
            }

            Iterator<SelectionKey> itr = keys.iterator();
            while (itr.hasNext()) {
                SelectionKey key = itr.next();
                itr.remove();

                if (!key.isValid()) {
                    continue;
                }

                try {
                    SelectableSocket socket = (SelectableSocket) key.attachment();
                    if (key.isAcceptable()) {
                        socket.onOperation(SelectionKey.OP_ACCEPT);
                    }
                    if (key.isConnectable()) {
                        socket.onOperation(SelectionKey.OP_CONNECT);
                    }
                    if (key.isReadable()) {
                        socket.onOperation(SelectionKey.OP_READ);
                    }
                    if (key.isWritable()) {
                        socket.onOperation(SelectionKey.OP_WRITE);
                    }
                } catch (CancelledKeyException ignore) {
                }
            }
        }

        for (SelectableSocket socket : mSockets) {
            try {
                socket.close();
            } catch (IOException ignore) {
            }
        }
        mSockets.clear();
    }
}
