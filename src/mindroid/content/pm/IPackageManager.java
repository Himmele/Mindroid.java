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

import java.util.List;
import mindroid.content.Intent;
import mindroid.os.Binder;
import mindroid.os.Bundle;
import mindroid.os.IBinder;
import mindroid.os.IInterface;
import mindroid.os.RemoteException;
import mindroid.util.concurrent.Promise;

public interface IPackageManager extends IInterface {
    public static abstract class Stub extends Binder implements IPackageManager {
        private static final String DESCRIPTOR = "mindroid://interfaces/mindroid/content/pm/IPackageManager";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static IPackageManager asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            return new IPackageManager.Proxy(binder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        protected void onTransact(int what, int num, Object obj, Bundle data, Promise<?> result) throws RemoteException {
            switch (what) {
            case MSG_GET_INSTALLED_PACKAGES: {
                List<PackageInfo> packages = getInstalledPackages(num);
                ((Promise<List<PackageInfo>>) result).complete(packages);
                break;
            }
            case MSG_GET_PACKAGE_INFO: {
                PackageInfo packageInfo = getPackageInfo((String) obj, num);
                ((Promise<PackageInfo>) result).complete(packageInfo);
                break;
            }
            case MSG_GET_PACKAGE_ARCHIVE_INFO: {
                PackageInfo packageInfo = getPackageArchiveInfo((String) obj, num);
                ((Promise<PackageInfo>) result).complete(packageInfo);
                break;
            }
            case MSG_RESOLVE_SERVICE: {
                ResolveInfo resolveInfo = resolveService((Intent) obj, num);
                ((Promise<ResolveInfo>) result).complete(resolveInfo);
                break;
            }
            case MSG_CHECK_PERMISSION: {
                int permission = checkPermission((String) obj, num);
                ((Promise<Integer>) result).complete(new Integer(permission));
                break;
            }
            case MSG_GET_PERMISSIONS: {
                String[] permissions = getPermissions(num);
                ((Promise<String[]>) result).complete(permissions);
                break;
            }
            default:
                super.onTransact(what, num, obj, data, result);
                break;
            }
        }

        private static class Proxy implements IPackageManager {
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
            public List<PackageInfo> getInstalledPackages(int flags) throws RemoteException {
                Promise<List<PackageInfo>> promise = new Promise<>();
                mRemote.transact(MSG_GET_INSTALLED_PACKAGES, flags, null, null, promise, 0);
                return Binder.get(promise);
            }

            @Override
            public PackageInfo getPackageInfo(String packageName, int flags) throws RemoteException {
                Promise<PackageInfo> promise = new Promise<>();
                mRemote.transact(MSG_GET_PACKAGE_INFO, flags, packageName, null, promise, 0);
                return Binder.get(promise);
            }

            @Override
            public PackageInfo getPackageArchiveInfo(String archiveFilePath, int flags) throws RemoteException {
                Promise<PackageInfo> promise = new Promise<>();
                mRemote.transact(MSG_GET_PACKAGE_ARCHIVE_INFO, flags, archiveFilePath, null, promise, 0);
                return Binder.get(promise);
            }

            @Override
            public ResolveInfo resolveService(Intent intent, int flags) throws RemoteException {
                Promise<ResolveInfo> promise = new Promise<>();
                mRemote.transact(MSG_RESOLVE_SERVICE, flags, intent, null, promise, 0);
                return Binder.get(promise);
            }

            @Override
            public int checkPermission(String permissionName, int pid) throws RemoteException {
                Promise<Integer> promise = new Promise<>();
                mRemote.transact(MSG_CHECK_PERMISSION, pid, permissionName, null, promise, 0);
                return Binder.get(promise);
            }

            @Override
            public String[] getPermissions(int pid) throws RemoteException {
                Promise<String[]> promise = new Promise<>();
                mRemote.transact(MSG_GET_PERMISSIONS, pid, null, null, promise, 0);
                return Binder.get(promise);
            }
        }

        static final int MSG_GET_INSTALLED_PACKAGES = 1;
        static final int MSG_GET_PACKAGE_INFO = 2;
        static final int MSG_GET_PACKAGE_ARCHIVE_INFO = 3;
        static final int MSG_RESOLVE_SERVICE = 4;
        static final int MSG_CHECK_PERMISSION = 5;
        static final int MSG_GET_PERMISSIONS = 6;
    }

    static class Proxy implements IPackageManager {
        private final IBinder mBinder;
        private final Stub mStub;
        private final IPackageManager mProxy;

        Proxy(IBinder binder) {
            mBinder = binder;
            if (binder.getUri().getScheme().equals("mindroid")) {
                mStub = (Stub) binder.queryLocalInterface(Stub.DESCRIPTOR);
                mProxy = new Stub.Proxy(binder);
            } else {
                mindroid.runtime.system.Runtime runtime = mindroid.runtime.system.Runtime.getRuntime();
                mStub = (Stub) runtime.getBinder(binder.getId());
                mProxy = (IPackageManager) runtime.getProxy(binder);
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
        public List<PackageInfo> getInstalledPackages(int flags) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                return mStub.getInstalledPackages(flags);
            } else {
                return mProxy.getInstalledPackages(flags);
            }
        }

        @Override
        public PackageInfo getPackageInfo(String packageName, int flags) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                return mStub.getPackageInfo(packageName, flags);
            } else {
                return mProxy.getPackageInfo(packageName, flags);
            }
        }

        @Override
        public PackageInfo getPackageArchiveInfo(String archiveFilePath, int flags) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                return mStub.getPackageArchiveInfo(archiveFilePath, flags);
            } else {
                return mProxy.getPackageArchiveInfo(archiveFilePath, flags);
            }
        }

        @Override
        public ResolveInfo resolveService(Intent intent, int flags) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                return mStub.resolveService(intent, flags);
            } else {
                return mProxy.resolveService(intent, flags);
            }
        }

        @Override
        public int checkPermission(String permissionName, int pid) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                return mStub.checkPermission(permissionName, pid);
            } else {
                return mProxy.checkPermission(permissionName, pid);
            }
        }

        @Override
        public String[] getPermissions(int pid) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                return mStub.getPermissions(pid);
            } else {
                return mProxy.getPermissions(pid);
            }
        }
    }

    public List<PackageInfo> getInstalledPackages(int flags) throws RemoteException;
    public PackageInfo getPackageInfo(String packageName, int flags) throws RemoteException;
    public PackageInfo getPackageArchiveInfo(String archiveFilePath, int flags) throws RemoteException;
    public ResolveInfo resolveService(Intent intent, int flags) throws RemoteException;
    public int checkPermission(String permissionName, int pid) throws RemoteException;
    public String[] getPermissions(int pid) throws RemoteException;
}
