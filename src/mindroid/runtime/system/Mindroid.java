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

package mindroid.runtime.system;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import mindroid.os.Binder;
import mindroid.os.Bundle;
import mindroid.os.IBinder;
import mindroid.os.IInterface;
import mindroid.os.Parcel;
import mindroid.os.RemoteException;
import mindroid.runtime.system.io.AbstractClient;
import mindroid.runtime.system.io.AbstractServer;
import mindroid.util.Log;
import mindroid.util.concurrent.CompletionException;
import mindroid.util.concurrent.Executors;
import mindroid.util.concurrent.Promise;

public class Mindroid extends Plugin {
    private static String LOG_TAG = "Mindroid";
    private static final String TIMEOUT = "timeout";
    private static final long DEFAULT_TRANSACTION_TIMEOUT = 10000;
    private static final boolean DEBUG = false;
    private static final ScheduledThreadPoolExecutor sExecutor;

    private ServiceDiscovery.Configuration mConfiguration;
    private Server mServer;
    private Map<Integer, Client> mClients = new ConcurrentHashMap<>();
    private final Map<Integer, Map<Long, WeakReference<IBinder>>> mProxies = new HashMap<>();

    static {
        sExecutor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("ThreadPoolExecutorDaemon");
                return t;
            }
        });
        sExecutor.setKeepAliveTime(10, TimeUnit.SECONDS);
        sExecutor.allowCoreThreadTimeOut(true);
        sExecutor.setRemoveOnCancelPolicy(true);
    }

    @Override
    public void start() {
        int nodeId = mRuntime.getNodeId();
        LOG_TAG = "Mindroid [" + nodeId + "]";
        mConfiguration = mRuntime.getConfiguration();
        if (mConfiguration != null) {
            ServiceDiscovery.Configuration.Node node = mConfiguration.nodes.get(nodeId);
            if (node != null) {
                ServiceDiscovery.Configuration.Plugin plugin = node.plugins.get("mindroid");
                if (plugin != null) {
                    ServiceDiscovery.Configuration.Server server = plugin.server;
                    if (server != null) {
                        mServer = new Server();
                        try {
                            mServer.start(server.uri);
                        } catch (IOException e) {
                            Log.println('E', LOG_TAG, e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void stop() {
        if (mServer != null) {
            mServer.shutdown(null);
        }
    }

    @Override
    public void attachBinder(Binder binder) {
    }

    @Override
    public void detachBinder(long id) {
    }

    @Override
    public synchronized void attachProxy(long proxyId, Binder.Proxy proxy) {
        int nodeId = (int) ((proxy.getId() >> 32) & 0xFFFFFFFFL);
        if (!mProxies.containsKey(nodeId)) {
            mProxies.put(nodeId, new HashMap<>());
        }
        mProxies.get(nodeId).put(proxyId, new WeakReference<>(proxy));
    }

    @Override
    public synchronized void detachProxy(long proxyId, long binderId) {
        // TODO: Lazy connection shutdown for clients without proxies.
//        int nodeId = (int) ((binderId >> 32) & 0xFFFFFFFFL);
//        if (mProxies.containsKey(nodeId)) {
//            Map<Long, WeakReference<IBinder>> proxies = mProxies.get(nodeId);
//            proxies.remove(proxyId);
//            if (proxies.isEmpty()) {
//                mProxies.remove(nodeId);
//                Client client = mClients.get(nodeId);
//                if (client != null) {
//                    client.shutdown(null);
//                    mClients.remove(nodeId);
//                }
//            }
//        } else {
//            Client client = mClients.get(nodeId);
//            if (client != null) {
//                client.shutdown(null);
//                mClients.remove(nodeId);
//            }
//        }
    }

    @Override
    public Binder getStub(Binder service) {
        return null;
    }

    @Override
    public IInterface getProxy(IBinder binder) {
        return null;
    }

    @Override
    public Promise<Void> connect(URI node, Bundle extras) {
        // Automatic connection establishment when referencing other nodes.
        return null;
    }

    @Override
    public Promise<Void> disconnect(URI node, Bundle extras) {
        return null;
    }

    @Override
    public Promise<Parcel> transact(IBinder binder, int what, Parcel data, int flags) throws RemoteException {
        int nodeId = (int) ((binder.getId() >> 32) & 0xFFFFFFFFL);
        Client client = mClients.get(nodeId);
        if (client == null) {
            if (mConfiguration != null) {
                ServiceDiscovery.Configuration.Node node = mConfiguration.nodes.get(nodeId);
                if (node != null) {
                    ServiceDiscovery.Configuration.Plugin plugin = node.plugins.get(binder.getUri().getScheme());
                    if (plugin != null) {
                        ServiceDiscovery.Configuration.Server server = plugin.server;
                        if (server != null) {
                            try {
                                client = new Client(node.id);
                                mClients.put(nodeId, client);
                                client.start(server.uri);
                            } catch (IOException e) {
                                mClients.remove(nodeId);
                                throw new RemoteException("Binder transaction failure");
                            }
                        } else {
                            throw new RemoteException("Binder transaction failure");
                        }
                    } else {
                        throw new RemoteException("Binder transaction failure");
                    }
                } else {
                    throw new RemoteException("Binder transaction failure");
                }
            } else {
                throw new RemoteException("Binder transaction failure");
            }
        }
        return client.transact(binder, what, data, flags);
    }

    @Override
    public void link(IBinder binder, IBinder.Supervisor supervisor, Bundle extras) throws RemoteException {
    }

    @Override
    public boolean unlink(IBinder binder, IBinder.Supervisor supervisor, Bundle extras) {
        return true;
    }

    public void onShutdown(AbstractClient client) {
        mClients.remove(client.getNodeId());
    }

    private static class Message {
        public static final int MESSAGE_TYPE_TRANSACTION = 1;
        public static final int MESSAGE_TYPE_EXCEPTION_TRANSACTION = 2;

        private Message(int type, String uri, int transactionId, int what, byte[] data, int size) {
            this(type, uri, transactionId, what, data, size, null);
        }

        private Message(int type, String uri, int transactionId, int what, byte[] data, int size, Throwable cause) {
            this.type = type;
            this.uri = uri;
            this.transactionId = transactionId;
            this.what = what;
            this.data = data;
            this.size = size;
            this.cause = cause;
        }

        public static Message newMessage(String uri, int transactionId, int what, byte[] data) {
            return newMessage(uri, transactionId, what, data, data.length);
        }

        public static Message newMessage(String uri, int transactionId, int what, byte[] data, int size) {
            return new Message(MESSAGE_TYPE_TRANSACTION, uri, transactionId, what, data, size);
        }

        public static Message newExceptionMessage(String uri, int transactionId, int what, byte[] data) {
            return newExceptionMessage(uri, transactionId, what, data, data.length, null);
        }

        public static Message newExceptionMessage(String uri, int transactionId, int what, byte[] data, Throwable cause) {
            return newExceptionMessage(uri, transactionId, what, data, data.length, cause);
        }

        public static Message newExceptionMessage(String uri, int transactionId, int what, byte[] data, int size) {
            return newExceptionMessage(uri, transactionId, what, data, data.length, null);
        }

        public static Message newExceptionMessage(String uri, int transactionId, int what, byte[] data, int size, Throwable cause) {
            return new Message(MESSAGE_TYPE_EXCEPTION_TRANSACTION, uri, transactionId, what, data, size, cause);
        }

        public static Message newMessage(DataInputStream inputStream) throws IOException {
            int type = inputStream.readInt();
            String uri = inputStream.readUTF();
            int transactionId = inputStream.readInt();
            int what = inputStream.readInt();
            int size = inputStream.readInt();
            byte[] data = new byte[size];
            inputStream.readFully(data, 0, size);
            if (type == MESSAGE_TYPE_TRANSACTION) {
                return new Message(type, uri, transactionId, what, data, size);
            } else {
                Throwable exception = null;
                int exceptionCount = inputStream.readInt();
                if (exceptionCount > 0) {
                    String exceptionClassName = inputStream.readUTF();
                    try {
                        exception = (Throwable) Class.forName(exceptionClassName).newInstance();
                    } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | ClassCastException e) {
                        exception = null;
                    }
                }
                return new Message(type, uri, transactionId, what, data, size, (exception != null) ? new RemoteException(exception) : new RemoteException());
            }
        }

        public final void write(DataOutputStream outputStream) throws IOException {
            synchronized (outputStream) {
                outputStream.writeInt(this.type);
                outputStream.writeUTF(this.uri);
                outputStream.writeInt(this.transactionId);
                outputStream.writeInt(this.what);
                outputStream.writeInt(this.size);
                outputStream.write(this.data, 0, this.size);
                if (type != MESSAGE_TYPE_TRANSACTION) {
                    if (this.cause != null && !RemoteException.class.isInstance(this.cause)) {
                        outputStream.writeInt(1);
                        outputStream.writeUTF(this.cause.getClass().getName());
                    } else {
                        outputStream.writeInt(0);
                    }
                }
                outputStream.flush();
            }
        }

        int type;
        String uri;
        int transactionId;
        int what;
        byte[] data;
        int size;
        Throwable cause;
    }

    private class Server extends AbstractServer {
        private final byte[] BINDER_TRANSACTION_FAILURE = "Binder transaction failure".getBytes(StandardCharsets.UTF_8);

        @Override
        public void onConnected(Connection connection) {
            Log.d(LOG_TAG, "Client connected from " + connection.getRemoteSocketAddress());
        }

        @Override
        public void onDisconnected(Connection connection, Throwable cause) {
            Log.d(LOG_TAG, "Client disconnected from " + connection.getRemoteSocketAddress());
        }

        @Override
        public void onTransact(Bundle context, InputStream inputStream, OutputStream outputStream) throws IOException {
            if (!context.containsKey("dataInputStream")) {
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                context.putObject("dataInputStream", dataInputStream);
            }
            if (!context.containsKey("datOutputStream")) {
                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                context.putObject("dataOutputStream", dataOutputStream);
            }
            DataInputStream dataInputStream = (DataInputStream) context.getObject("dataInputStream");
            DataOutputStream dataOutputStream = (DataOutputStream) context.getObject("dataOutputStream");

            try {
                Message message = Message.newMessage(dataInputStream);

                if (message.type == Message.MESSAGE_TYPE_TRANSACTION) {
                    try {
                        IBinder binder = mRuntime.getBinder(URI.create(message.uri));
                        if (binder != null) {
                            Promise<Parcel> result = binder.transact(message.what, Parcel.obtain(message.data), 0);
                            if (result != null) {
                                result.then((value, exception) -> {
                                    try {
                                        if (exception == null) {
                                            Message.newMessage(message.uri, message.transactionId, message.what, value.getByteArray(), value.size()).write(dataOutputStream);
                                        } else {
                                            final Throwable cause;
                                            if (exception instanceof CompletionException && exception.getCause() != null) {
                                                cause = exception.getCause();
                                            } else {
                                                cause = exception;
                                            }
                                            Message.newExceptionMessage(message.uri, message.transactionId, message.what, BINDER_TRANSACTION_FAILURE, cause).write(dataOutputStream);
                                        }
                                    } catch (IOException e) {
                                        try {
                                            ((Closeable) context.getObject("connection")).close();
                                        } catch (IOException ignore) {
                                        }
                                    }
                                });
                            }
                        } else {
                            Message.newExceptionMessage(message.uri, message.transactionId, message.what, BINDER_TRANSACTION_FAILURE, new RemoteException("Invalid service URI")).write(dataOutputStream);
                        }
                    } catch (IllegalArgumentException e) {
                        Log.e(LOG_TAG, e.getMessage(), e);
                        Message.newExceptionMessage(message.uri, message.transactionId, message.what, BINDER_TRANSACTION_FAILURE, e).write(dataOutputStream);
                    } catch (RemoteException e) {
                        Log.e(LOG_TAG, e.getMessage(), e);
                        Message.newExceptionMessage(message.uri, message.transactionId, message.what, BINDER_TRANSACTION_FAILURE, e).write(dataOutputStream);
                    }
                } else {
                    Log.e(LOG_TAG, "Invalid message type: " + message.type);
                }
            } catch (IOException e) {
                if (DEBUG) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                }
                throw e;
            }
        }
    }

    private class Client extends AbstractClient {
        private final AtomicInteger mTransactionIdGenerator = new AtomicInteger(1);
        private Map<Integer, Promise<Parcel>> mTransactions = new ConcurrentHashMap<>();

        public Client(int nodeId) throws IOException {
            super(nodeId);
        }

        public void shutdown(Throwable cause) {
            Mindroid.this.onShutdown(this);

            if (mTransactions != null) {
                for (Promise<Parcel> promise : mTransactions.values()) {
                    promise.completeWith(new RemoteException());
                }
            }

            sExecutor.execute(() -> { super.shutdown(cause); });
        }

        public Promise<Parcel> transact(IBinder binder, int what, Parcel data, int flags) throws RemoteException {
            final int transactionId = mTransactionIdGenerator.getAndIncrement();
            Promise<Parcel> result;
            try {
                Bundle context = getContext();
                if (!context.containsKey("datOutputStream")) {
                    DataOutputStream dataOutputStream = new DataOutputStream(getOutputStream());
                    context.putObject("dataOutputStream", dataOutputStream);
                }
                DataOutputStream dataOutputStream = (DataOutputStream) context.getObject("dataOutputStream");

                if (flags == Binder.FLAG_ONEWAY) {
                    result = null;
                } else {
                    final Promise<Parcel> promise = new Promise<>(Executors.SYNCHRONOUS_EXECUTOR);
                    result = promise.orTimeout(data.getLongExtra(TIMEOUT, DEFAULT_TRANSACTION_TIMEOUT))
                            .then((value, exception) -> {
                                mTransactions.remove(transactionId);
                            });
                    mTransactions.put(transactionId, promise);
                }

                Message.newMessage(binder.getUri().toString(), transactionId, what, data.getByteArray(), data.size()).write(dataOutputStream);
            } catch (IOException e) {
                mTransactions.remove(transactionId);
                shutdown(e);
                throw new RemoteException("Binder transaction failure", e);
            }
            return result;
        }

        @Override
        public void onConnected() {
            Log.d(LOG_TAG, "Connected to " + getRemoteSocketAddress());
        }

        @Override
        public void onDisconnected(Throwable cause) {
            Log.d(LOG_TAG, "Disconnected from " + getRemoteSocketAddress());
        }

        @Override
        public void onTransact(Bundle context, InputStream inputStream, OutputStream outputStream) throws IOException {
            if (!context.containsKey("dataInputStream")) {
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                context.putObject("dataInputStream", dataInputStream);
            }
            DataInputStream dataInputStream = (DataInputStream) context.getObject("dataInputStream");

            try {
                Message message = Message.newMessage(dataInputStream);

                final Promise<Parcel> promise = mTransactions.get(message.transactionId);
                if (promise != null) {
                    mTransactions.remove(message.transactionId);
                    if (message.type == Message.MESSAGE_TYPE_TRANSACTION) {
                        promise.complete(Parcel.obtain(message.data).asInput());
                    } else {
                        promise.completeWith(message.cause);
                    }
                } else {
                    Log.e(LOG_TAG, "Invalid transaction id: " + message.transactionId);
                }
            } catch (IOException e) {
                if (DEBUG) {
                    Log.e(LOG_TAG, e.getMessage(), e);
                }
                throw e;
            }
        }
    }
}
