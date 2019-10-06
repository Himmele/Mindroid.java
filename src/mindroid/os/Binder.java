/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (C) 2013 Daniel Himmelein
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

package mindroid.os;

import mindroid.util.Log;
import mindroid.util.concurrent.CancellationException;
import mindroid.util.concurrent.ExecutionException;
import mindroid.util.concurrent.Executors;
import mindroid.util.concurrent.Promise;
import mindroid.util.concurrent.TimeoutException;
import mindroid.runtime.system.Runtime;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Base class for a remotable object, the core part of a lightweight remote procedure call mechanism
 * defined by {@link IBinder}. This class is an implementation of IBinder that provides the standard
 * support creating a local implementation of such an object.
 *
 * You can derive directly from Binder to implement your own custom RPC protocol or simply
 * instantiate a raw Binder object directly to use as a token that can be shared across processes.
 *
 * @see IBinder
 */
public class Binder implements IBinder {
    private static final String LOG_TAG = "Binder";
    private static final int TRANSACTION = 1;
    private static final int LIGHTWEIGHT_TRANSACTION = 2;
    private static final String EXCEPTION_MESSAGE = "Binder transaction failure";
    private static final ThreadLocal<Integer> sCallingPid = new ThreadLocal<>();
    private final Runtime mRuntime;
    private long mId;
    private final IMessenger mTarget;
    private IInterface mOwner;
    private String mDescriptor;
    private URI mUri;

    public Binder() {
        mRuntime = Runtime.getRuntime();
        mId = mRuntime.attachBinder(this);
        mTarget = new Messenger();
        setCallingPid(Process.myPid());
    }

    public Binder(final Looper looper) {
        mRuntime = Runtime.getRuntime();
        mId = mRuntime.attachBinder(this);
        mTarget = new Messenger(looper);
        setCallingPid(Process.myPid());
    }

    public Binder(final Executor executor) {
        mRuntime = Runtime.getRuntime();
        mId = mRuntime.attachBinder(this);
        mTarget = new ExecutorMessenger(executor);
        setCallingPid(Process.myPid());
    }

    /** @hide */
    public Binder(Binder binder) {
        mRuntime = binder.mRuntime;
        mId = binder.mId;
        if (binder.mTarget instanceof Messenger) {
            mTarget = new Messenger(((Messenger) binder.mTarget).mHandler.mLooper);
        } else {
            mTarget = binder.mTarget;
        }
        setCallingPid(Process.myPid());
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            mRuntime.detachBinder(mId, mUri);
        } finally {
            super.finalize();
        }
    }

    @Override
    public final long getId() {
        return mId & 0xFFFFFFFFL;
    }

    @Override
    public final URI getUri() {
        return mUri;
    }

    /**
     * Convenience method for associating a specific interface with the Binder. After calling,
     * queryInterface() will be implemented for you to return the given owner IInterface when the
     * corresponding descriptor is requested.
     */
    public void attachInterface(IInterface owner, String descriptor) {
        mOwner = owner;
        mDescriptor = descriptor;

        try {
            URI uri = new URI(mDescriptor);
            if (uri.getScheme() == null) {
                throw new URISyntaxException(mDescriptor, "Scheme must not be null");
            }
            int nodeId = (int) ((mId >> 32) & 0xFFFFFFFFL);
            int id = (int) (mId & 0xFFFFFFFFL);
            mUri = new URI(uri.getScheme(), String.valueOf(nodeId) + "." + String.valueOf(id), null, null, null);
            mRuntime.attachBinder(mUri, this);
        } catch (URISyntaxException e) {
            Log.e(LOG_TAG, "Failed to attach interface to runtime system", e);
        }
    }

    /**
     * Default implementation returns an empty interface name.
     */
    @Override
    public String getInterfaceDescriptor() {
        return mDescriptor;
    }

    /**
     * Use information supplied to attachInterface() to return the associated IInterface if it
     * matches the requested descriptor.
     */
    @Override
    public IInterface queryLocalInterface(String descriptor) {
        if (mDescriptor.equals(descriptor)) {
            return mOwner;
        }
        return null;
    }

    /**
     * Returns true if the current thread is this binder's thread.
     */
    public final boolean isCurrentThread() {
        return mTarget.isCurrentThread();
    }

    /**
     * Return the ID of the process that sent you the current transaction
     * that is being processed.  This pid can be used with higher-level
     * system services to determine its identity and check permissions.
     * If the current thread is not currently executing an incoming transaction,
     * then its own pid is returned.
     */
    public static final int getCallingPid() {
        Integer callingPid = sCallingPid.get();
        if (callingPid != null) {
            return callingPid.intValue();
        } else {
            return 0;
        }
    }

    private static final int setCallingPid(int pid) {
        int origPid = 0;
        Integer callingPid = sCallingPid.get();
        if (callingPid != null) {
            origPid = callingPid.intValue();
        }
        sCallingPid.set(pid);
        return origPid;
    }

    /**
     * Default implementation is a stub that returns null. You will want to override this to do the
     * appropriate unmarshalling of transactions.
     *
     * <p>
     * If you want to call this, call transact().
     */
    protected void onTransact(int what, Parcel data, Promise<Parcel> result) throws RemoteException {
        throw new RemoteException(new NoSuchMethodException());
    }

    protected void onTransact(int what, int num, Object obj, Bundle data, Promise<?> result) throws RemoteException {
        throw new RemoteException(new NoSuchMethodException());
    }

    /**
     * Default implementations rewinds the parcels and calls onTransact. On the remote side,
     * transact calls into the binder to do the IPC.
     */
    @Override
    public Promise<Parcel> transact(int what, Parcel data, int flags) throws RemoteException {
        if (data != null) {
            data.asInput();
        }
        Message message = Message.obtain();
        message.what = TRANSACTION;
        message.arg1 = what;
        message.obj = data;
        message.sendingPid = Process.myPid();
        Promise<Parcel> promise;
        if (flags == FLAG_ONEWAY) {
            message.result = null;
            promise = null;
        } else {
            Promise<Parcel> p = new Promise<Parcel>(Executors.SYNCHRONOUS_EXECUTOR);
            message.result = p;
            promise = p.then(parcel -> {
                parcel.asInput();
            });
        }
        if (!mTarget.send(message)) {
            throw new RemoteException(EXCEPTION_MESSAGE);
        }
        return promise;
    }

    @Override
    public final void transact(int what, int num, Object obj, Bundle data, Promise<?> promise, int flags) throws RemoteException {
        Message message = Message.obtain();
        message.what = LIGHTWEIGHT_TRANSACTION;
        message.arg1 = what;
        message.arg2 = num;
        message.obj = obj;
        message.setData(data);
        message.result = promise;
        message.sendingPid = Process.myPid();
        if (!mTarget.send(message)) {
            throw new RemoteException(EXCEPTION_MESSAGE);
        }
    }

    private final void onTransact(final Message message) {
        final int origPid = setCallingPid(message.sendingPid);
        try {
            switch (message.what) {
            case TRANSACTION:
                onTransact(message.arg1, (Parcel) message.obj, (Promise<Parcel>) message.result);
                break;
            case LIGHTWEIGHT_TRANSACTION:
                onTransact(message.arg1, message.arg2, message.obj, message.peekData(), message.result);
                break;
            default:
                break;
            }
        } catch (RemoteException e) {
            Throwable caughtException = checkException(e);
            if (message.result != null) {
                message.result.completeWith(caughtException);
            } else {
                Log.w(LOG_TAG, EXCEPTION_MESSAGE, e);
            }
        } catch (RuntimeException e) {
            Throwable caughtException = checkException(e);
            if (message.result != null) {
                message.result.completeWith(caughtException);
            } else {
                Log.w(LOG_TAG, EXCEPTION_MESSAGE, e);
            }
        } finally {
            message.result = null;
            setCallingPid(origPid);
        }
    }

    private final Throwable checkException(Exception e) throws RuntimeException {
        Throwable caughtException = null;
        if (e instanceof SecurityException) {
            caughtException = e;
        } else if (e instanceof IllegalArgumentException) {
            caughtException = e;
        } else if (e instanceof IllegalStateException) {
            caughtException = e;
        } else if (e instanceof UnsupportedOperationException) {
            caughtException = e;
        } else if (e instanceof RemoteException) {
            caughtException = e;
        }
        if (caughtException == null) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
        return caughtException;
    }

    @Override
    public void link(Supervisor supervisor, Bundle extras) throws RemoteException {
    }

    @Override
    public boolean unlink(Supervisor supervisor, Bundle extras) {
        return true;
    }

    @Override
    public void dispose() {
    }

    private interface IMessenger {
        public boolean isCurrentThread();
        public boolean send(final Message message);
    }

    private class Messenger implements IMessenger {
        private final Handler mHandler;

        public Messenger() {
            this(Looper.myLooper());
        }

        public Messenger(Looper looper) {
            mHandler = new Handler(looper) {
                @Override
                public void handleMessage(Message message) {
                    onTransact(message);
                }
            };
        }

        @Override
        public boolean isCurrentThread() {
            return mHandler.getLooper().isCurrentThread();
        }

        @Override
        public boolean send(final Message message) {
            return mHandler.sendMessage(message);
        }
    }

    private class ExecutorMessenger implements IMessenger {
        private final Executor mExecutor;

        public ExecutorMessenger(final Executor executor) {
            mExecutor = executor;
        }

        @Override
        public boolean isCurrentThread() {
            return false;
        }

        @Override
        public boolean send(final Message message) {
            try {
                mExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        onTransact(message);
                    }
                });
                return true;
            } catch (RejectedExecutionException e) {
                return false;
            }
        }
    }

    public static final <T> T get(Promise<T> result) throws RemoteException {
        try {
            return result.get();
        } catch (CancellationException e) {
            throw new RemoteException(EXCEPTION_MESSAGE);
        } catch (ExecutionException e) {
            if (e.getCause() != null) {
                if (e.getCause() instanceof RemoteException) {
                    throw (RemoteException) e.getCause();
                } else if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
            }
            throw new RemoteException(EXCEPTION_MESSAGE, e.getCause());
        } catch (InterruptedException e) {
            // Important: Do not quietly eat an interrupt() event,
            // but re-interrupt the thread for other blocking calls
            // to be interrupted as well.
            Thread.currentThread().interrupt();
            throw new RemoteException(EXCEPTION_MESSAGE);
        }
    }

    public static final <T> T get(Promise<T> result, long timeout) throws RemoteException {
        try {
            return result.get(timeout);
        } catch (CancellationException | TimeoutException e) {
            throw new RemoteException(EXCEPTION_MESSAGE);
        } catch (ExecutionException e) {
            if (e.getCause() != null) {
                if (e.getCause() instanceof RemoteException) {
                    throw (RemoteException) e.getCause();
                } else if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
            }
            throw new RemoteException(EXCEPTION_MESSAGE, e.getCause());
        } catch (InterruptedException e) {
            // Important: Do not quietly eat an interrupt() event,
            // but re-interrupt the thread for other blocking calls
            // to be interrupted as well.
            Thread.currentThread().interrupt();
            throw new RemoteException(EXCEPTION_MESSAGE);
        }
    }

    public static final class Proxy implements IBinder {
        private static final String EXCEPTION_MESSAGE = "Binder transaction failure";
        private volatile Runtime mRuntime;
        private final long mProxyId;
        private final long mId;
        private String mDescriptor;
        private URI mUri;

        public Proxy(URI uri) throws IllegalArgumentException {
            if (uri == null) {
                throw new IllegalArgumentException("Invalid URI: " + uri);
            }
            mRuntime = Runtime.getRuntime();
            String authority = uri.getAuthority();
            String[] parts = authority.split("\\.");
            if (parts.length == 2) {
                try {
                    mId = ((long) Integer.valueOf(parts[0]) << 32) | ((long) Integer.valueOf(parts[1]) & 0xFFFFFFFFL);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid URI: " + uri.toString());
                }
            } else {
                throw new IllegalArgumentException("Invalid URI: " + uri.toString());
            }
            String path = uri.getPath();
            if (path != null && !path.isEmpty()) {
                String[] pairs = path.substring(1).split(",");
                for (String pair : pairs) {
                    int i = pair.indexOf("=");
                    if (i >= 0) {
                        String key = pair.substring(0, i).trim();
                        String value = pair.substring(i + 1).trim();
                        if (key.equals("if")) {
                            mDescriptor = uri.getScheme() + "://interfaces/" + value;
                            if (uri.getQuery() != null) {
                                mDescriptor += "?" + uri.getQuery();
                            }
                            break;
                        }
                    }
                }
            }
            if (mDescriptor == null) {
                throw new IllegalArgumentException("Invalid URI: " + uri.toString());
            }
            mUri = URI.create(uri.getScheme() + "://" + uri.getAuthority());
            mProxyId = mRuntime.attachProxy(this);
        }

        @Override
        protected void finalize() throws Throwable {
            try {
                dispose();
            } finally {
                super.finalize();
            }
        }

        @Override
        public long getId() {
            return mId;
        }

        @Override
        public final URI getUri() {
            return mUri;
        }

        @Override
        public String getInterfaceDescriptor() {
            return mDescriptor;
        }

        @Override
        public IInterface queryLocalInterface(String descriptor) {
            return null;
        }

        @Override
        public Promise<Parcel> transact(int what, Parcel data, int flags) throws RemoteException {
            final Runtime runtime = mRuntime;
            if (runtime != null) {
                return runtime.transact(this, what, data, flags);
            } else {
                throw new RemoteException(EXCEPTION_MESSAGE + ": Invalid proxy");
            }
        }

        public void transact(int what, int num, Object obj, Bundle data, Promise<?> promise, int flags) throws RemoteException {
            throw new RemoteException(EXCEPTION_MESSAGE);
        }

        @Override
        public void link(Supervisor supervisor, Bundle extras) throws RemoteException {
            final Runtime runtime = mRuntime;
            if (runtime != null) {
                runtime.link(this, supervisor, extras);
            } else {
                throw new RemoteException(EXCEPTION_MESSAGE + ": Invalid proxy");
            }
        }

        @Override
        public boolean unlink(Supervisor supervisor, Bundle extras) {
            final Runtime runtime = mRuntime;
            if (runtime != null) {
                return runtime.unlink(this, supervisor, extras);
            } else {
                return true;
            }
        }

        @Override
        public synchronized void dispose() {
            final Runtime runtime = mRuntime;
            if (runtime != null) {
                runtime.detachProxy(mId, mUri, mProxyId);
                mRuntime = null;
            }
        }
    }

    /** @hide */
    public void setId(long id) {
        mId = id;
        mUri = URI.create(mUri.getScheme() + "://" + String.valueOf((int) ((mId >> 32) & 0xFFFFFFFFL)) + "." + String.valueOf((int) (mId & 0xFFFFFFFFL)));
    }
}
