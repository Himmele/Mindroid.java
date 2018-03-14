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
        private static final String DESCRIPTOR = "mindroid://interfaces/mindroid/content/pm/IPackageInstaller";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static IPackageInstaller asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            return new IPackageInstaller.Proxy(binder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        protected void onTransact(int what, int num, Object obj, Bundle data, Promise<?> result) throws RemoteException {
            switch (what) {
            case MSG_INSTALL: {
                if (data.containsKey("app")) {
                    String app = data.getString("app");
                    install(new File(app));
                } else {
                    String packageName = data.getString("packageName");
                    String[] classNames = data.getStringArray("classNames");
                    install(packageName, classNames);
                }
                ((Promise<Void>) result).complete(null);
                break;
            }
            case MSG_UNINSTALL: {
                String packageName = (String) obj;
                uninstall(packageName);
                break;
            }
            default:
                super.onTransact(what, num, obj, data, result);
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
            public void install(File app) throws RemoteException {
                Promise<Void> promise = new Promise<>();
                Bundle data = new Bundle();
                data.putString("app", app.getAbsolutePath());
                mRemote.transact(MSG_INSTALL, 0, 0, data, promise, 0);
                Binder.get(promise);
            }

            @Override
            public void install(String packageName, String[] classNames) throws RemoteException {
                Promise<Void> promise = new Promise<>();
                Bundle data = new Bundle();
                data.putString("packageName", packageName);
                data.putStringArray("classNames", classNames);
                mRemote.transact(MSG_INSTALL, 0, null, data, promise, 0);
                Binder.get(promise);
            }

            @Override
            public void uninstall(String packageName) throws RemoteException {
                mRemote.transact(MSG_UNINSTALL, 0, packageName, null, null, FLAG_ONEWAY);
            }
        }

        static final int MSG_INSTALL = 1;
        static final int MSG_UNINSTALL = 2;
    }

    static class Proxy implements IPackageInstaller {
        private final IBinder mBinder;
        private final Stub mStub;
        private final IPackageInstaller mProxy;

        Proxy(IBinder binder) {
            mBinder = binder;
            if (binder.getUri().getScheme().equals("mindroid")) {
                mStub = (Stub) binder.queryLocalInterface(Stub.DESCRIPTOR);
                mProxy = new Stub.Proxy(binder);
            } else {
                mindroid.runtime.system.Runtime runtime = mindroid.runtime.system.Runtime.getRuntime();
                mStub = (Stub) runtime.getBinder(binder.getId());
                mProxy = (IPackageInstaller) runtime.getProxy(binder);
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
        public void install(File app) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                mStub.install(app);
            } else {
                mProxy.install(app);
            }
        }

        @Override
        public void install(String packageName, String[] classNames) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                mStub.install(packageName, classNames);
            } else {
                mProxy.install(packageName, classNames);
            }
        }

        @Override
        public void uninstall(String packageName) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                mStub.uninstall(packageName);
            } else {
                mProxy.uninstall(packageName);
            }
        }
    }

    public void install(File app) throws RemoteException;
    public void install(String packageName, String[] classNames) throws RemoteException;
    public void uninstall(String packageName) throws RemoteException;
}
