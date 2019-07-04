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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import mindroid.os.Bundle;
import mindroid.util.Log;

public abstract class AbstractServer {
    private String LOG_TAG;
    private static final boolean DEBUG = false;

    private final SocketExecutorGroup mExecutorGroup = new SocketExecutorGroup();
    private final Set<Connection> mConnections = ConcurrentHashMap.newKeySet();
    private ServerSocket mServerSocket;

    public void start(String uri) throws IOException {
        LOG_TAG = "Server [" + uri + "]";
        URI url;
        try {
            url = new URI(uri);
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URI: " + uri);
        }

        if ("tcp".equals(url.getScheme())) {
            try {
                mServerSocket = new ServerSocket(new InetSocketAddress(InetAddress.getByName(url.getHost()), url.getPort()));
                mServerSocket.setListener((operation, argument) -> {
                    if (operation == ServerSocket.OP_ACCEPT) {
                        mServerSocket.accept().whenComplete((socket, exception) -> {
                            if (exception == null) {
                                if (DEBUG) {
                                    try {
                                        Log.d(LOG_TAG, "New connection from " + socket.getRemoteAddress());
                                    } catch (IOException ignore) {
                                    }
                                }
                                mConnections.add(new Connection(socket));
                            } else {
                                shutdown(exception);
                            }
                        });
                    }
                });
                mExecutorGroup.register(mServerSocket);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Cannot bind to server socket on port " + url.getPort());
            }
        } else {
            throw new IllegalArgumentException("Invalid URI scheme: " + url.getScheme());
        }
    }

    public void shutdown(Throwable cause) {
        mExecutorGroup.unregister(mServerSocket);
        try {
            mServerSocket.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Cannot close server socket", e);
        }

        for (Connection connection : mConnections) {
            try {
                connection.close();
            } catch (IOException ignore) {
            }
        }

        mExecutorGroup.shutdown();
        onShutdown(cause);
    }

    public Set<Connection> getConnections() {
        return Collections.unmodifiableSet(mConnections);
    }

    public abstract void onConnected(Connection connection);

    public abstract void onDisconnected(Connection connection, Throwable cause);

    public abstract boolean onTransact(Bundle context, InputStream inputStream, OutputStream outputStream) throws IOException;

    public void onShutdown(Throwable cause) {
    }

    public class Connection implements Closeable {
        private final Bundle mContext = new Bundle();
        private final Socket mSocket;
        private final InputStream mInputStream;
        private final OutputStream mOutputStream;

        Connection(Socket socket) {
            mContext.putObject("connection", this);
            mSocket = socket;
            mInputStream = mSocket.getInputStream();
            mOutputStream = mSocket.getOutputStream();
            mSocket.setListener((operation, argument) -> {
                if (operation == Socket.OP_READ) {
                    try {
                        while (mInputStream.available() > 0) {
                            if (!AbstractServer.this.onTransact(mContext, mInputStream, mOutputStream)) {
                                break;
                            }
                        }
                    } catch (IOException e) {
                        if (DEBUG) {
                            Log.e(LOG_TAG, e.getMessage(), e);
                        }
                        try {
                            close(e);
                        } catch (IOException ignore) {
                        }
                    }
                } else if (operation == Socket.OP_CLOSE) {
                    if (DEBUG) {
                        if (argument != null) {
                            Exception e = (Exception) argument;
                            Log.e(LOG_TAG, "Socket has been closed: " + e.getMessage(), e);
                        } else {
                            Log.e(LOG_TAG, "Socket has been closed");
                        }
                    }
                    try {
                        close();
                    } catch (IOException ignore) {
                    }
                }
            });
            mExecutorGroup.register(mSocket);

            AbstractServer.this.onConnected(this);
        }

        @Override
        public void close() throws IOException {
            close(null);
        }

        private void close(Throwable cause) throws IOException {
            if (DEBUG) {
                Log.d(LOG_TAG, "Disconnecting from " + mSocket.getRemoteAddress());
            }

            mExecutorGroup.unregister(mSocket);
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Cannot close socket", e);
            }
            if (mInputStream != null) {
                try {
                    mInputStream.close();
                } catch (IOException ignore) {
                }
            }
            if (mOutputStream != null) {
                try {
                    mOutputStream.close();
                } catch (IOException ignore) {
                }
            }
            mConnections.remove(Connection.this);
            if (DEBUG) {
                Log.d(LOG_TAG, "Disconnected from " + mSocket.getRemoteAddress());
            }

            AbstractServer.this.onDisconnected(this, cause);
        }

        public Bundle getContext() {
            return mContext;
        }

        public InputStream getInputStream() {
            return mInputStream;
        }

        public OutputStream getOutputStream() {
            return mOutputStream;
        }

        public SocketAddress getLocalSocketAddress() throws IOException {
            return mSocket.getLocalAddress();
        }

        public SocketAddress getRemoteSocketAddress() throws IOException {
            return mSocket.getRemoteAddress();
        }
    }
}
