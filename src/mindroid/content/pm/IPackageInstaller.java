/*
 * Copyright (C) 2017 Daniel Himmelein
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

import java.io.File;
import mindroid.os.Binder;
import mindroid.os.Bundle;
import mindroid.os.IBinder;
import mindroid.os.IInterface;
import mindroid.os.RemoteException;
import mindroid.util.concurrent.Promise;

public interface IPackageInstaller extends IInterface {
    public static abstract class Stub extends Binder implements IPackageInstaller {
        private static final java.lang.String DESCRIPTOR = "mindroid.content.pm.IPackageInstaller";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static IPackageInstaller asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            return new IPackageInstaller.Stub.SmartProxy(binder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        protected void onTransact(int what, int arg1, int arg2, Object obj, Bundle data, Promise<?> result) throws RemoteException {
            switch (what) {
            case MSG_INSTALL: {
                install((File) obj);
                ((Promise<Void>) result).complete(null);
                break;
            }
            case MSG_UNINSTALL: {
                uninstall((String) obj);
                break;
            }
            default:
                super.onTransact(what, arg1, arg2, obj, data, result);
                break;
            }
        }

        private static class Proxy implements IPackageInstaller {
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
                if (obj instanceof Proxy) {
                    final Proxy that = (Proxy) obj;
                    return this.mRemote.equals(that.mRemote);
                }
                return false;
            }

            @Override
            public int hashCode() {
                return mRemote.hashCode();
            }

            @Override
            public void install(File app) throws RemoteException {
                Promise<Void> promise = new Promise<>();
                mRemote.transact(MSG_INSTALL, app, promise, 0);
                Binder.get(promise);
            }

            @Override
            public void uninstall(String packageName) throws RemoteException {
                mRemote.transact(MSG_UNINSTALL, packageName, null, FLAG_ONEWAY);
            }
        }

        private static class SmartProxy implements IPackageInstaller {
            private final IBinder mRemote;
            private final IPackageInstaller mStub;
            private final IPackageInstaller mProxy;

            SmartProxy(IBinder remote) {
                mRemote = remote;
                mStub = (IPackageInstaller) remote.queryLocalInterface(DESCRIPTOR);
                mProxy = new IPackageInstaller.Stub.Proxy(remote);
            }

            @Override
            public IBinder asBinder() {
                return mRemote;
            }

            @Override
            public boolean equals(final Object obj) {
                if (obj == null) return false;
                if (obj == this) return true;
                if (obj instanceof SmartProxy) {
                    final SmartProxy that = (SmartProxy) obj;
                    return this.mRemote.equals(that.mRemote);
                }
                return false;
            }

            @Override
            public int hashCode() {
                return mRemote.hashCode();
            }

            @Override
            public void install(File app) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    mStub.install(app);
                } else {
                    mProxy.install(app);
                }
            }

            @Override
            public void uninstall(String packageName) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    mStub.uninstall(packageName);
                } else {
                    mProxy.uninstall(packageName);
                }
            }
        }

        static final int MSG_INSTALL = 1;
        static final int MSG_UNINSTALL = 2;
    }

    public void install(File app) throws RemoteException;
    public void uninstall(String packageName) throws RemoteException;
}
