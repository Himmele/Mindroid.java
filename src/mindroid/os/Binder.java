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
import mindroid.util.concurrent.Promise;
import mindroid.util.concurrent.TimeoutException;
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
    private static final String EXCEPTION_MESSAGE = "Binder transaction failure";
    private static final ThreadLocal<Integer> sCallingPid = new ThreadLocal<>();
    private final IMessenger mTarget;
    private IInterface mOwner;
    private String mDescriptor;

    public Binder() {
        mTarget = new Messenger();
        setCallingPid(Process.myPid());
    }

    public Binder(final Looper looper) {
        mTarget = new Messenger(looper);
        setCallingPid(Process.myPid());
    }

    public Binder(final Executor executor) {
        mTarget = new ExecutorMessenger(executor);
        setCallingPid(Process.myPid());
    }
    
    /**
     * Convenience method for associating a specific interface with the Binder. After calling,
     * queryInterface() will be implemented for you to return the given owner IInterface when the
     * corresponding descriptor is requested.
     */
    public void attachInterface(IInterface owner, String descriptor) {
        mOwner = owner;
        mDescriptor = descriptor;
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
    protected void onTransact(int what, int arg1, int arg2, Object obj, Bundle data, Promise<?> result) throws RemoteException {
    }

    /**
     * Default implementations rewinds the parcels and calls onTransact. On the remote side,
     * transact calls into the binder to do the IPC.
     */
    @Override
    public final void transact(int what, Promise<?> promise, int flags) throws RemoteException {
        Message message = Message.obtain();
        message.what = what;
        message.result = promise;
        transact(message, flags);
    }

    @Override
    public final void transact(int what, Object obj, Promise<?> promise, int flags) throws RemoteException {
        Message message = Message.obtain();
        message.what = what;
        message.obj = obj;
        message.result = promise;
        transact(message, flags);
    }

    @Override
    public final void transact(int what, int arg1, int arg2, Promise<?> promise, int flags) throws RemoteException {
        Message message = Message.obtain();
        message.what = what;
        message.arg1 = arg1;
        message.arg2 = arg2;
        message.result = promise;
        transact(message, flags);
    }

    @Override
    public final void transact(int what, int arg1, int arg2, Object obj, Promise<?> promise, int flags) throws RemoteException {
        Message message = Message.obtain();
        message.what = what;
        message.arg1 = arg1;
        message.arg2 = arg2;
        message.obj = obj;
        message.result = promise;
        transact(message, flags);
    }

    @Override
    public final void transact(int what, Bundle data, Promise<?> promise, int flags) throws RemoteException {
        Message message = Message.obtain();
        message.what = what;
        message.setData(data);
        message.result = promise;
        transact(message, flags);
    }

    @Override
    public final void transact(int what, int arg1, int arg2, Bundle data, Promise<?> promise, int flags) throws RemoteException {
        Message message = Message.obtain();
        message.what = what;
        message.arg1 = arg1;
        message.arg2 = arg2;
        message.setData(data);
        message.result = promise;
        transact(message, flags);
    }

    private final void transact(Message message, int flags) throws RemoteException {
        message.sendingPid = Process.myPid();
        if (!mTarget.send(message)) {
            throw new RemoteException(EXCEPTION_MESSAGE);
        }
    }

    private final void onTransact(final Message message) {
        final int origPid = setCallingPid(message.sendingPid);
        try {
            onTransact(message.what, message.arg1, message.arg2, message.obj, message.peekData(), message.result);
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
            setCallingPid(origPid);
        }
    }

    @Override
    public final boolean runsOnSameThread() {
        return mTarget.runsOnSameThread();
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

    private interface IMessenger {
        public boolean runsOnSameThread();
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
        public boolean runsOnSameThread() {
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
        public boolean runsOnSameThread() {
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
}
