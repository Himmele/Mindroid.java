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

package mindroid.runtime.system.plugins.xmlrpc;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.net.URI;
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
import mindroid.runtime.system.Configuration;
import mindroid.runtime.system.Plugin;
import mindroid.runtime.system.io.AbstractClient;
import mindroid.runtime.system.io.AbstractServer;
import mindroid.util.Log;
import mindroid.util.concurrent.Executors;
import mindroid.util.concurrent.Promise;

public class XmlRpc extends Plugin {
    private static final String LOG_TAG = "XmlRpc";
    private static final String TIMEOUT = "timeout";
    private static final long DEFAULT_TRANSACTION_TIMEOUT = 10000;
    private static final boolean DEBUG = false;
    private static final ScheduledThreadPoolExecutor sExecutor;

    private Configuration.Plugin mConfiguration;
    private Server mServer;
    private Map<Integer, Client> mClients = new HashMap<>();
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
        Configuration configuration = mRuntime.getConfiguration();
        if (configuration != null) {
            mConfiguration = configuration.plugins.get("xmlrpc");
            if (mConfiguration != null) {
                Configuration.Node node = mConfiguration.nodes.get(nodeId);
                try {
                    mServer = new Server(node.uri);
                } catch (IOException e) {
                    Log.println('E', LOG_TAG, e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void stop() {
        mServer.shutdown();
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
//                    client.shutdown();
//                    mClients.remove(nodeId);
//                }
//            }
//        } else {
//            Client client = mClients.get(nodeId);
//            if (client != null) {
//                client.shutdown();
//                mClients.remove(nodeId);
//            }
//        }
    }

    @Override
    public Binder getStub(Binder service) {
        String interfaceDescriptor = service.getInterfaceDescriptor();
        interfaceDescriptor = interfaceDescriptor.replaceFirst(".*://", "xmlrpc://");
        String className;
        if (interfaceDescriptor.equals("xmlrpc://interfaces/examples/eliza/IEliza")) {
            className = "examples.eliza.xmlrpc.IEliza$Stub";
        } else if (interfaceDescriptor.equals("xmlrpc://interfaces/examples/eliza/IElizaListener")) {
            className = "examples.eliza.xmlrpc.IElizaListener$Stub";
        } else {
            return null;
        }

        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> ctor = clazz.getConstructor(Binder.class);
            Object o = ctor.newInstance(service);
            return (Binder) o;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public IInterface getProxy(IBinder binder) {
        String className;
        if (binder.getInterfaceDescriptor().equals("xmlrpc://interfaces/examples/eliza/IEliza")) {
            className = "examples.eliza.xmlrpc.IEliza$Stub$Proxy";
        } else if (binder.getInterfaceDescriptor().equals("xmlrpc://interfaces/examples/eliza/IElizaListener")) {
            className = "examples.eliza.xmlrpc.IElizaListener$Stub$Proxy";
        } else {
            return null;
        }

        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> ctor = clazz.getConstructor(IBinder.class);
            Object o = ctor.newInstance(binder);
            return (IInterface) o;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Promise<Parcel> transact(IBinder binder, int what, Parcel data, int flags) throws RemoteException {
        int nodeId = (int) ((binder.getId() >> 32) & 0xFFFFFFFFL);
        AbstractClient client = mClients.get(nodeId);
        if (client == null) {
            Configuration.Node node;
            if (mConfiguration != null && (node = mConfiguration.nodes.get(nodeId)) != null) {
                if (!mClients.containsKey(nodeId)) {
                    try {
                        mClients.put(nodeId, new Client(node.id, node.uri));
                    } catch (IOException e) {
                        throw new RemoteException("Binder transaction failure");
                    }
                }
                client = mClients.get(nodeId);
            } else {
                throw new RemoteException("Binder transaction failure");
            }
        }
        return client.transact(binder, what, data, flags);
    }

    public void onShutdown(AbstractClient client) {
        mClients.remove(client.getNodeId());
    }

    private static class Message {
        public static final int MESSAGE_TYPE_TRANSACTION = 1;
        public static final int MESSAGE_TYPE_EXCEPTION_TRANSACTION = 2;

        private Message(int type, String uri, int transactionId, int what, byte[] data) {
            this.type = type;
            this.uri = uri;
            this.transactionId = transactionId;
            this.what = what;
            this.data = data;
        }

        public static Message newMessage(String uri, int transactionId, int what, byte[] data) {
            return new Message(MESSAGE_TYPE_TRANSACTION, uri, transactionId, what, data);
        }

        public static Message newExceptionMessage(String uri, int transactionId, int what, byte[] data) {
            return new Message(MESSAGE_TYPE_EXCEPTION_TRANSACTION, uri, transactionId, what, data);
        }

        public static Message newMessage(DataInputStream inputStream) throws IOException {
            int type = inputStream.readInt();
            String uri = inputStream.readUTF();
            int transactionId = inputStream.readInt();
            int what = inputStream.readInt();
            int size = inputStream.readInt();
            byte[] data = new byte[size];
            inputStream.readFully(data, 0, size);
            return new Message(type, uri, transactionId, what, data);
        }

        public final void write(DataOutputStream outputStream) throws IOException {
            outputStream.writeInt(this.type);
            outputStream.writeUTF(this.uri);
            outputStream.writeInt(this.transactionId);
            outputStream.writeInt(this.what);
            outputStream.writeInt(this.data.length);
            outputStream.write(this.data);
            outputStream.flush();
        }

        int type;
        String uri;
        int transactionId;
        int what;
        byte[] data;
    }

    private class Server extends AbstractServer {
        private final byte[] BINDER_TRANSACTION_FAILURE = "Binder transaction failure".getBytes();

        public Server(String uri) throws IOException {
            super(uri);
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
                                            Message.newMessage(message.uri, message.transactionId, message.what, value.toByteArray()).write(dataOutputStream);
                                        } else {
                                            Message.newExceptionMessage(message.uri, message.transactionId, message.what, BINDER_TRANSACTION_FAILURE).write(dataOutputStream);
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
                            Message.newExceptionMessage(message.uri, message.transactionId, message.what, BINDER_TRANSACTION_FAILURE).write(dataOutputStream);
                        }
                    } catch (IllegalArgumentException e) {
                        Log.e(LOG_TAG, e.getMessage(), e);
                        Message.newExceptionMessage(message.uri, message.transactionId, message.what, BINDER_TRANSACTION_FAILURE).write(dataOutputStream);
                    } catch (RemoteException e) {
                        Log.e(LOG_TAG, e.getMessage(), e);
                        Message.newExceptionMessage(message.uri, message.transactionId, message.what, BINDER_TRANSACTION_FAILURE).write(dataOutputStream);
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

        public Client(int nodeId, String uri) throws IOException {
            super(nodeId, uri);
        }

        protected void shutdown() {
            XmlRpc.this.onShutdown(this);

            sExecutor.execute(() -> { super.shutdown(); });

            for (Promise<Parcel> promise : mTransactions.values()) {
                promise.completeWith(new RemoteException());
            }
        }

        @Override
        public Promise<Parcel> transact(IBinder binder, int what, Parcel data, int flags) throws RemoteException {
            Bundle context = getContext();
            if (!context.containsKey("datOutputStream")) {
                DataOutputStream dataOutputStream = new DataOutputStream(getConnection().getOutputStream());
                context.putObject("dataOutputStream", dataOutputStream);
            }
            DataOutputStream dataOutputStream = (DataOutputStream) context.getObject("dataOutputStream");

            final int transactionId = mTransactionIdGenerator.getAndIncrement();
            Promise<Parcel> result;
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

            try {
                Message.newMessage(binder.getUri().toString(), transactionId, what, data.toByteArray()).write(dataOutputStream);
            } catch (IOException e) {
                if (result != null) {
                    result.completeWith(e);
                    mTransactions.remove(transactionId);
                }
                shutdown();
                throw new RemoteException(e);
            }
            return result;
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
                        promise.completeWith(new RemoteException());
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
