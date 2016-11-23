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

import mindroid.util.concurrent.CancellationException;
import mindroid.util.concurrent.ExecutionException;
import mindroid.util.concurrent.Executor;
import mindroid.util.concurrent.Promise;

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
	private static final String EXCEPTION_MESSAGE = "Binder transaction failure";
	private final IMessenger mTarget;
	private IInterface mOwner;
	private String mDescriptor;

    public Binder() {
        mTarget = new Messenger();
    }

    public Binder(final Executor executor) {
        mTarget = new ThreadPoolMessenger(executor);
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
	public String getInterfaceDescriptor() {
		return mDescriptor;
	}

	/**
	 * Use information supplied to attachInterface() to return the associated IInterface if it
	 * matches the requested descriptor.
	 */
	public IInterface queryLocalInterface(String descriptor) {
		if (mDescriptor.equals(descriptor)) {
			return mOwner;
		}
		return null;
	}

	/**
	 * Default implementations rewinds the parcels and calls onTransact. On the remote side,
	 * transact calls into the binder to do the IPC.
	 */
	public Object transact(int what, int flags) throws RemoteException {
	    Message message = Message.obtain();
        message.what = what;
		if (flags == FLAG_ONEWAY) {
		    mTarget.send(message);
			return null;
		} else {
			Promise promise = new Promise();
			message.result = promise;
			mTarget.send(message);
			try {
				return promise.get();
			} catch (CancellationException e) {
				throw new RemoteException(EXCEPTION_MESSAGE);
			} catch (ExecutionException e) {
				throw new RemoteException(EXCEPTION_MESSAGE);
			} catch (InterruptedException e) {
				throw new RemoteException(EXCEPTION_MESSAGE);
			}
		}
	}

	public Object transact(int what, Object obj, int flags) throws RemoteException {
		Message message = Message.obtain();
        message.what = what;
        message.obj = obj;
		if (flags == FLAG_ONEWAY) {
		    mTarget.send(message);
			return null;
		} else {
			Promise promise = new Promise();
			message.result = promise;
			mTarget.send(message);
			try {
				return promise.get();
			} catch (CancellationException e) {
				throw new RemoteException(EXCEPTION_MESSAGE);
			} catch (ExecutionException e) {
				throw new RemoteException(EXCEPTION_MESSAGE);
			} catch (InterruptedException e) {
				throw new RemoteException(EXCEPTION_MESSAGE);
			}
		}
	}

	public Object transact(int what, int arg1, int arg2, int flags) throws RemoteException {
		Message message = Message.obtain();
		message.what = what;
        message.arg1 = arg1;
        message.arg2 = arg2;
		if (flags == FLAG_ONEWAY) {
		    mTarget.send(message);
			return null;
		} else {
			Promise promise = new Promise();
			message.result = promise;
			mTarget.send(message);
			try {
				return promise.get();
			} catch (CancellationException e) {
				throw new RemoteException(EXCEPTION_MESSAGE);
			} catch (ExecutionException e) {
				throw new RemoteException(EXCEPTION_MESSAGE);
			} catch (InterruptedException e) {
				throw new RemoteException(EXCEPTION_MESSAGE);
			}
		}
	}

	public Object transact(int what, int arg1, int arg2, Object obj, int flags) throws RemoteException {
		Message message = Message.obtain();
		message.what = what;
        message.arg1 = arg1;
        message.arg2 = arg2;
        message.obj = obj;
		if (flags == FLAG_ONEWAY) {
		    mTarget.send(message);
			return null;
		} else {
			Promise promise = new Promise();
			message.result = promise;
			mTarget.send(message);
			try {
				return promise.get();
			} catch (CancellationException e) {
				throw new RemoteException(EXCEPTION_MESSAGE);
			} catch (ExecutionException e) {
				throw new RemoteException(EXCEPTION_MESSAGE);
			} catch (InterruptedException e) {
				throw new RemoteException(EXCEPTION_MESSAGE);
			}
		}
	}

	public Object transact(int what, Bundle data, int flags) throws RemoteException {
		Message message = Message.obtain();
		message.what = what;
		message.setData(data);
		if (flags == FLAG_ONEWAY) {
		    mTarget.send(message);
			return null;
		} else {
			Promise promise = new Promise();
			message.result = promise;
			mTarget.send(message);
			try {
				return promise.get();
			} catch (CancellationException e) {
				throw new RemoteException(EXCEPTION_MESSAGE);
			} catch (ExecutionException e) {
				throw new RemoteException(EXCEPTION_MESSAGE);
			} catch (InterruptedException e) {
				throw new RemoteException(EXCEPTION_MESSAGE);
			}
		}
	}

	public Object transact(int what, int arg1, int arg2, Bundle data, int flags) throws RemoteException {
		Message message = Message.obtain();
		message.what = what;
		message.arg1 = arg1;
		message.arg2 = arg2;
		message.setData(data);
		if (flags == FLAG_ONEWAY) {
		    mTarget.send(message);
			return null;
		} else {
			Promise promise = new Promise();
			message.result = promise;
			mTarget.send(message);
			try {
				return promise.get();
			} catch (CancellationException e) {
				throw new RemoteException(EXCEPTION_MESSAGE);
			} catch (ExecutionException e) {
				throw new RemoteException(EXCEPTION_MESSAGE);
			} catch (InterruptedException e) {
				throw new RemoteException(EXCEPTION_MESSAGE);
			}
		}
	}

	public boolean runsOnSameThread() {
	    return mTarget.runsOnSameThread();
	}

	/**
	 * Default implementation is a stub that returns null. You will want to override this to do the
	 * appropriate unmarshalling of transactions.
	 * 
	 * <p>
	 * If you want to call this, call transact().
	 */
	protected Object onTransact(int what, int arg1, int arg2, Object obj, Bundle data) throws RemoteException {
		return null;
	}

    private interface IMessenger {
        public boolean runsOnSameThread();
        public void send(final Message message);
    }

    private class Messenger implements IMessenger {
        private final Handler mHandler = new Handler() {
            public void handleMessage(Message message) {
                try {
                    Object o = onTransact(message.what, message.arg1, message.arg2, message.obj, message.peekData());
                    if (message.result != null) {
                        ((Promise) message.result).set(o);
                    }
                } catch (RemoteException e) {
                }
            }
        };

        public boolean runsOnSameThread() {
            return mHandler.getLooper().isCurrentThread();
        }

        public void send(final Message message) {
            message.setTarget(mHandler);
            message.sendToTarget();
        }
    }

    private class ThreadPoolMessenger implements IMessenger {
        private final Executor mExecutor;

        public ThreadPoolMessenger(final Executor executor) {
            mExecutor = executor;
        }

        public void send(final Message message) {
            mExecutor.execute(new Runnable() {
                public void run() {
                    try {
                        Object o = onTransact(message.what, message.arg1, message.arg2, message.obj, message.peekData());
                        if (message.result != null) {
                            ((Promise) message.result).set(o);
                        }
                    } catch (RemoteException e) {
                    }
                }
            });
        }

        public boolean runsOnSameThread() {
            return false;
        }
    }
}
