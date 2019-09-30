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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import mindroid.os.Bundle;
import mindroid.util.Log;

public abstract class AbstractClient {
    private String LOG_TAG;
    private static final int SHUTDOWN_TIMEOUT = 10000; //ms
    private static final int CONNECTION_ESTABLISHMENT_TIMEOUT = 10000;
    private static final boolean DEBUG = false;

    private final int mNodeId;
    private final Socket mSocket;
    private String mHost;
    private int mPort;
    private Connection mConnection;

    public AbstractClient(int nodeId) throws IOException {
        mNodeId = nodeId;
        mSocket = new Socket();
    }

    public void start(String uri) throws IOException {
        LOG_TAG = "Client [" + uri + "]";

        try {
            URI url = new URI(uri);
            if (!"tcp".equals(url.getScheme())) {
                throw new IllegalArgumentException("Invalid URI scheme: " + url.getScheme());
            }
            mHost = url.getHost();
            mPort = url.getPort();

            try {
                mSocket.connect(new InetSocketAddress(mHost, mPort), CONNECTION_ESTABLISHMENT_TIMEOUT);
                mConnection = new Connection(mSocket);
                onConnected();
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                shutdown(e);
                throw e;
            }
        } catch (URISyntaxException e) {
            shutdown(e);
            throw new IOException("Invalid URI: " + uri);
        } catch (RuntimeException e) {
            shutdown(e);
            throw e;
        }
    }

    public void shutdown(Throwable cause) {
        if (mConnection != null) {
            try {
                mConnection.close();
            } catch (IOException ignore) {
            }
        }

        onDisconnected(cause);
    }

    public int getNodeId() {
        return mNodeId;
    }

    public abstract void onConnected();

    public abstract void onDisconnected(Throwable cause);

    public abstract void onTransact(Bundle context, InputStream inputStream, OutputStream outputStream) throws IOException;

    public Bundle getContext() throws IOException {
        return mConnection.mContext;
    }

    public InputStream getInputStream() throws IOException {
        return mConnection.mInputStream;
    }

    public OutputStream getOutputStream() throws IOException {
        return mConnection.mOutputStream;
    }

    public SocketAddress getLocalSocketAddress() {
        return mSocket.getLocalSocketAddress();
    }

    public SocketAddress getRemoteSocketAddress() {
        return mSocket.getRemoteSocketAddress();
    }

    public class Connection extends Thread implements Closeable {
        private final Bundle mContext = new Bundle();
        private final Socket mSocket;
        private final InputStream mInputStream;
        private final OutputStream mOutputStream;

        Connection(Socket socket) throws IOException {
            setName("Client: " + socket.getLocalSocketAddress() + " <<>> " + socket.getRemoteSocketAddress());
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
            super.start();
        }

        @Override
        public void close() throws IOException {
            if (DEBUG) {
                Log.d(LOG_TAG, "Closing connection");
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
            if (DEBUG) {
                Log.d(LOG_TAG, "Connection has been closed");
            }
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
                    AbstractClient.this.onTransact(mContext, mInputStream, mOutputStream);
                } catch (IOException e) {
                    if (DEBUG) {
                        Log.e(LOG_TAG, e.getMessage(), e);
                    }
                    shutdown(e);
                    break;
                }
            }

            if (DEBUG) {
                Log.d(LOG_TAG, "Connection has been terminated");
            }
        }
    }
}
