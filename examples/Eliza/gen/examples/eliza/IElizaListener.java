/*
 * Copyright (C) 2018 ESR Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package examples.eliza;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import mindroid.os.Binder;
import mindroid.os.IBinder;
import mindroid.os.IInterface;
import mindroid.os.Parcel;
import mindroid.os.RemoteException;
import mindroid.util.concurrent.Future;
import mindroid.util.concurrent.Promise;

public interface IElizaListener extends IInterface {
    public static abstract class Stub extends Binder implements IElizaListener {
        public static final String DESCRIPTOR = "mindroid://interfaces/examples/eliza/IElizaListener";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static IElizaListener asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            return new IElizaListener.Proxy(binder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        protected void onTransact(int what, Parcel data, Promise<Parcel> result) throws RemoteException {
            switch (what) {
            case MSG_ON_REPLY: {
                String _reply = data.getString();
                onReply(_reply);
                break;
            }
            default:
                super.onTransact(what, data, result);
                break;
            }
        }

        private static class Proxy implements IElizaListener {
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
            public void onReply(String reply) throws RemoteException {
                Parcel _data = Parcel.obtain();
                _data.putString(reply);
                mRemote.transact(MSG_ON_REPLY, _data, FLAG_ONEWAY);
            }
        }

        static final int MSG_ON_REPLY = 1;
    }

    static class Proxy implements IElizaListener {
        private final IBinder mBinder;
        private final Stub mStub;
        private final IElizaListener mProxy;

        Proxy(IBinder binder) {
            mBinder = binder;
            if (binder.getUri().getScheme().equals("mindroid")) {
                mStub = (Stub) binder.queryLocalInterface(Stub.DESCRIPTOR);
                mProxy = new Stub.Proxy(binder);
            } else {
                mindroid.runtime.system.Runtime runtime = mindroid.runtime.system.Runtime.getRuntime();
                mStub = (Stub) runtime.getBinder(binder.getId());
                mProxy = (IElizaListener) runtime.getProxy(binder);
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
        public void onReply(String reply) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                mStub.onReply(reply);
            } else {
                mProxy.onReply(reply);
            }
        }
    }

    public void onReply(String reply) throws RemoteException;
}
