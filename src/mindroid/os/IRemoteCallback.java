/*
 * Copyright (C) 2016 Daniel Himmelein
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

import mindroid.util.concurrent.Promise;

public interface IRemoteCallback extends IInterface {
    public static abstract class Stub extends Binder implements IRemoteCallback {
        private static final String DESCRIPTOR = "mindroid://interfaces/mindroid/os/IRemoteCallback";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static IRemoteCallback asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            return new IRemoteCallback.Proxy(binder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        protected void onTransact(int what, int num, Object obj, Bundle data, Promise<?> result) throws RemoteException {
            switch (what) {
            case MSG_SEND_RESULT: {
                sendResult(data);
                break;
            }
            default:
                super.onTransact(what, num, obj, data, result);
                break;
            }
        }

        private static class Proxy implements IRemoteCallback {
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
            public void sendResult(Bundle data) throws RemoteException {
                mRemote.transact(MSG_SEND_RESULT, 0, null, data, null, FLAG_ONEWAY);
            }
        }

        static final int MSG_SEND_RESULT = 1;
    }

    static class Proxy implements IRemoteCallback {
        private final IBinder mBinder;
        private final Stub mStub;
        private final IRemoteCallback mProxy;

        Proxy(IBinder binder) {
            mBinder = binder;
            if (binder.getUri().getScheme().equals("mindroid")) {
                mStub = (Stub) binder.queryLocalInterface(Stub.DESCRIPTOR);
                mProxy = new Stub.Proxy(binder);
            } else {
                mindroid.runtime.system.Runtime runtime = mindroid.runtime.system.Runtime.getRuntime();
                mStub = (Stub) runtime.getBinder(binder.getId());
                mProxy = (IRemoteCallback) runtime.getProxy(binder);
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
        public void sendResult(Bundle data) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                mStub.sendResult(data);
            } else {
                mProxy.sendResult(data);
            }
        }
    }

    public void sendResult(Bundle data) throws RemoteException;
}
