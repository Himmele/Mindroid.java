/*
 * Copyright (C) 2018 Daniel Himmelein
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

package examples.eliza;

import mindroid.os.Binder;
import mindroid.os.IBinder;
import mindroid.os.IInterface;
import mindroid.os.Parcel;
import mindroid.os.RemoteException;
import mindroid.util.concurrent.Future;
import mindroid.util.concurrent.Promise;

public interface IEliza extends IInterface {
    public static abstract class Stub extends Binder implements IEliza {
        private static final String DESCRIPTOR = "mindroid://interfaces/examples/eliza/IEliza";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static IEliza asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            return new IEliza.Proxy(binder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        protected void onTransact(int what, Parcel data, Promise<Parcel> result) throws RemoteException {
            switch (what) {
            case MSG_ASK1: {
                String question = data.getString();
                String reply = ask1(question);
                Parcel parcel = Parcel.obtain();
                try {
                    parcel.putString(reply);
                    result.complete(parcel);
                } catch (RemoteException e) {
                    result.completeWith(e);
                }
                break;
            }
            case MSG_ASK2: {
                String question = data.getString();
                Future<String> reply = ask2(question);
                reply.then((value, exception) -> {
                    if (exception == null) {
                        Parcel parcel = Parcel.obtain();
                        try {
                            parcel.putString(value);
                            result.complete(parcel);
                        } catch (RemoteException e) {
                            result.completeWith(e);
                        }
                    } else {
                        result.completeWith(exception);
                    }
                });
                break;
            }
            case MSG_ASK3: {
                String question = data.getString();
                IBinder binder = data.getBinder();
                ask3(question, IElizaListener.Stub.asInterface(binder));
                break;
            }
            default:
                super.onTransact(what, data, result);
                break;
            }
        }

        private static class Proxy implements IEliza {
            private final IBinder mRemote;

            Proxy(IBinder remote) {
                mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return mRemote;
            }

            @Override
            public boolean equals(final Object obj) {
                if (obj == null) return false;
                if (obj == this) return true;
                if (obj instanceof Stub.Proxy) {
                    final Stub.Proxy that = (Stub.Proxy) obj;
                    return this.mRemote.equals(that.mRemote);
                }
                return false;
            }

            @Override
            public int hashCode() {
                return mRemote.hashCode();
            }

            @Override
            public String ask1(String question) throws RemoteException {
                Promise<String> promise = new Promise<>();
                Parcel data = Parcel.obtain();
                data.putString(question);
                mRemote.transact(MSG_ASK1, data, 0)
                        .then((parcel, exception) -> {
                            if (exception == null) {
                                try {
                                    String reply = parcel.getString();
                                    promise.complete(reply);
                                } catch (RemoteException e) {
                                    promise.completeWith(e);
                                }
                            } else {
                                promise.completeWith(exception);
                            }
                        });
                return Binder.get(promise);
            }

            @Override
            public Future<String> ask2(String question) throws RemoteException {
                Promise<String> promise = new Promise<>();
                Parcel data = Parcel.obtain();
                data.putString(question);
                mRemote.transact(MSG_ASK2, data, 0)
                        .then((parcel, exception) -> {
                            if (exception == null) {
                                try {
                                    String reply = parcel.getString();
                                    promise.complete(reply);
                                } catch (RemoteException e) {
                                    promise.completeWith(e);
                                }
                            } else {
                                promise.completeWith(exception);
                            }
                        });
                return promise;
            }

            @Override
            public void ask3(String question, IElizaListener listener) throws RemoteException {
                Parcel data = Parcel.obtain();
                data.putString(question);
                data.putBinder(mRemote, listener.asBinder());
                mRemote.transact(MSG_ASK3, data, FLAG_ONEWAY);
            }
        }

        static final int MSG_ASK1 = 1;
        static final int MSG_ASK2 = 2;
        static final int MSG_ASK3 = 3;
    }

    static class Proxy implements IEliza {
        private final IBinder mBinder;
        private final Stub mStub;
        private final IEliza mProxy;

        Proxy(IBinder binder) {
            mBinder = binder;
            if (binder.getUri().getScheme().equals("mindroid")) {
                mStub = (Stub) binder.queryLocalInterface(Stub.DESCRIPTOR);
                mProxy = new Stub.Proxy(binder);
            } else {
                mindroid.runtime.system.Runtime runtime = mindroid.runtime.system.Runtime.getRuntime();
                mStub = (Stub) runtime.getBinder(binder.getId());
                mProxy = (IEliza) runtime.getProxy(binder);
            }
        }

        @Override
        public IBinder asBinder() {
            return mBinder;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) return false;
            if (obj == this) return true;
            if (obj instanceof Proxy) {
                final Proxy that = (Proxy) obj;
                return this.mBinder.equals(that.mBinder);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return mBinder.hashCode();
        }

        @Override
        public String ask1(String question) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                return mStub.ask1(question);
            } else {
                return mProxy.ask1(question);
            }
        }

        @Override
        public Future<String> ask2(String question) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                return mStub.ask2(question);
            } else {
                return mProxy.ask2(question);
            }
        }

        @Override
        public void ask3(String question, IElizaListener listener) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                mStub.ask3(question, IElizaListener.Stub.asInterface(listener.asBinder()));
            } else {
                mProxy.ask3(question, listener);
            }
        }
    }

    public String ask1(String question) throws RemoteException;
    public Future<String> ask2(String question) throws RemoteException;
    public void ask3(String question, IElizaListener listener) throws RemoteException;
}
