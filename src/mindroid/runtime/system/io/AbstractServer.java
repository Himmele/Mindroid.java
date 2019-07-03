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

package mindroid.runtime.system.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import mindroid.os.Bundle;
import mindroid.util.Log;

public abstract class AbstractServer {
    private String LOG_TAG;
    private static final int SHUTDOWN_TIMEOUT = 10000; //ms
    private static final boolean DEBUG = false;

    private final Set<Connection> mConnections = ConcurrentHashMap.newKeySet();
    private ServerSocket mServerSocket;
    private Thread mThread;

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
                mServerSocket = new ServerSocket();
                mServerSocket.setReuseAddress(true);
                mServerSocket.bind(new InetSocketAddress(InetAddress.getByName(url.getHost()), url.getPort()));

                mThread = new Thread("Server [" + mServerSocket.getLocalSocketAddress() + "]") {
                    public void run() {
                        while (!isInterrupted()) {
                            try {
                                Socket socket = mServerSocket.accept();
                                if (DEBUG) {
                                    Log.d(LOG_TAG, "New connection from " + socket.getRemoteSocketAddress());
                                }
                                mConnections.add(new Connection(socket));
                            } catch (IOException e) {
                                Log.e(LOG_TAG, e.getMessage(), e);
                            }
                        }
                    }
                };
                mThread.start();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Cannot bind to server socket on port " + url.getPort());
            }
        } else {
            throw new IllegalArgumentException("Invalid URI scheme: " + url.getScheme());
        }
    }

    public void shutdown() {
        shutdown(null);
    }

    public void shutdown(Throwable cause) {
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

        mThread.interrupt();
        try {
            mThread.join(SHUTDOWN_TIMEOUT);
        } catch (InterruptedException ignore) {
        }
        if (mThread.isAlive()) {
            Log.e(LOG_TAG, "Cannot shutdown server");
        }

        onShutdown(cause);
    }

    public Set<Connection> getConnections() {
        return Collections.unmodifiableSet(mConnections);
    }

    public abstract void onConnected(Connection connection);

    public abstract void onDisconnected(Connection connection, Throwable cause);

    public abstract void onTransact(Bundle context, InputStream inputStream, OutputStream outputStream) throws IOException;

    public void onShutdown(Throwable cause) {
    }

    public class Connection extends Thread implements Closeable {
        private final Bundle mContext = new Bundle();
        private final Socket mSocket;
        private final InputStream mInputStream;
        private final OutputStream mOutputStream;

        Connection(Socket socket) throws IOException {
            mContext.putObject("connection", this);
            mSocket = socket;
            try {
                mInputStream = socket.getInputStream();
                mOutputStream = socket.getOutputStream();
            } catch (IOException e) {
                Log.d(LOG_TAG, "Failed to set up connection", e);
                try {
                    close();
                } catch (IOException ignore) {
                }
                throw e;
            }

            AbstractServer.this.onConnected(this);
            super.start();
        }

        @Override
        public void close() throws IOException {
            close(null);
        }

        private void close(Throwable cause) throws IOException {
            if (DEBUG) {
                Log.d(LOG_TAG, "Disconnecting from " + mSocket.getRemoteSocketAddress());
            }

            interrupt();
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
            try {
                join(SHUTDOWN_TIMEOUT);
                if (isAlive()) {
                    Log.e(LOG_TAG, "Cannot shutdown connection");
                }
            } catch (InterruptedException ignore) {
            }
            mConnections.remove(this);
            if (DEBUG) {
                Log.d(LOG_TAG, "Disconnected from " + mSocket.getRemoteSocketAddress());
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

        public void run() {
            while (!isInterrupted()) {
                try {
                    AbstractServer.this.onTransact(mContext, mInputStream, mOutputStream);
                } catch (IOException e) {
                    if (DEBUG) {
                        Log.e(LOG_TAG, e.getMessage(), e);
                    }
                    try {
                        close(e);
                    } catch (IOException ignore) {
                    }
                    break;
                }
            }

            if (DEBUG) {
                Log.d(LOG_TAG, "Connection has been terminated");
            }
        }
    }
}
