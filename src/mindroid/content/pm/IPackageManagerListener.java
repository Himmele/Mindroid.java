/*
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

package mindroid.content.pm;

import mindroid.os.Bundle;
import mindroid.os.IInterface;
import mindroid.os.IBinder;
import mindroid.os.Binder;
import mindroid.os.RemoteException;
import mindroid.util.concurrent.Promise;

public interface IPackageManagerListener extends IInterface {
    public static abstract class Stub extends Binder implements IPackageManagerListener {
        private static final String DESCRIPTOR = "mindroid://interfaces/mindroid/content/pm/IPackageManagerListener";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static mindroid.content.pm.IPackageManagerListener asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            return new IPackageManagerListener.Proxy(binder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        protected void onTransact(int what, int num, Object obj, Bundle data, Promise<?> result) throws RemoteException {
            switch (what) {
            case MSG_NOTIFY_BOOT_COMPLETED: {
                onBootCompleted();
                break;
            }
            default:
                super.onTransact(what, num, obj, data, result);
                break;
            }
        }

        private static class Proxy implements IPackageManagerListener {
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
            public void onBootCompleted() throws RemoteException {
                mRemote.transact(MSG_NOTIFY_BOOT_COMPLETED, 0, null, null, null, FLAG_ONEWAY);
            }
        }

        static final int MSG_NOTIFY_BOOT_COMPLETED = 1;
    }

    static class Proxy implements IPackageManagerListener {
        private final IBinder mBinder;
        private final Stub mStub;
        private final IPackageManagerListener mProxy;

        Proxy(IBinder binder) {
            mBinder = binder;
            if (binder.getUri().getScheme().equals("mindroid")) {
                mStub = (Stub) binder.queryLocalInterface(Stub.DESCRIPTOR);
                mProxy = new Stub.Proxy(binder);
            } else {
                mindroid.runtime.system.Runtime runtime = mindroid.runtime.system.Runtime.getRuntime();
                mStub = (Stub) runtime.getBinder(binder.getId());
                mProxy = (IPackageManagerListener) runtime.getProxy(binder);
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
        public void onBootCompleted() throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                mStub.onBootCompleted();
            } else {
                mProxy.onBootCompleted();
            }
        }
    }

    public void onBootCompleted() throws RemoteException;
}
