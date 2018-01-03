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
        private static final java.lang.String DESCRIPTOR = "mindroid.content.pm.IPackageManager";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static IPackageManager asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            return new IPackageManager.Stub.SmartProxy(binder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        protected void onTransact(int what, int arg1, int arg2, Object obj, Bundle data, Promise<?> result) throws RemoteException {
            switch (what) {
            case MSG_GET_INSTALLED_PACKAGES: {
                List<PackageInfo> packages = getInstalledPackages(arg1);
                ((Promise<List<PackageInfo>>) result).set(packages);
                break;
            }
            case MSG_GET_PACKAGE_INFO: {
                String packageName = (String) obj;
                int flags = arg1;
                PackageInfo packageInfo = getPackageInfo(packageName, flags);
                ((Promise<PackageInfo>) result).set(packageInfo);
                break;
            }
            case MSG_GET_PACKAGE_ARCHIVE_INFO: {
                String archiveFilePath = (String) obj;
                int flags = arg1;
                PackageInfo packageInfo = getPackageArchiveInfo(archiveFilePath, flags);
                ((Promise<PackageInfo>) result).set(packageInfo);
                break;
            }
            case MSG_RESOLVE_SERVICE: {
                Intent intent = (Intent) obj;
                ResolveInfo resolveInfo = resolveService(intent, arg1);
                ((Promise<ResolveInfo>) result).set(resolveInfo);
                break;
            }
            case MSG_CHECK_PERMISSION: {
                int permission = checkPermission((String) obj, arg1);
                ((Promise<Integer>) result).set(new Integer(permission));
                break;
            }
            case MSG_GET_PERMISSIONS: {
                String[] permissions = getPermissions(arg1);
                ((Promise<String[]>) result).set(permissions);
                break;
            }
            case MSG_ADD_LISTENER: {
                addListener(IPackageManagerListener.Stub.asInterface((IBinder) obj));
                break;
            }
            case MSG_REMOVE_LISTENER: {
                removeListener(IPackageManagerListener.Stub.asInterface((IBinder) obj));
                break;
            }
            default:
                super.onTransact(what, arg1, arg2, obj, data, result);
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
            public List<PackageInfo> getInstalledPackages(int flags) throws RemoteException {
                Promise<List<PackageInfo>> promise = new Promise<>();
                mRemote.transact(MSG_GET_INSTALLED_PACKAGES, flags, 0, promise, 0);
                return Binder.get(promise);
            }

            @Override
            public PackageInfo getPackageInfo(String packageName, int flags) throws RemoteException {
                Promise<PackageInfo> promise = new Promise<>();
                mRemote.transact(MSG_GET_PACKAGE_INFO, flags, 0, packageName, promise, 0);
                return Binder.get(promise);
            }

            @Override
            public PackageInfo getPackageArchiveInfo(String archiveFilePath, int flags) throws RemoteException {
                Promise<PackageInfo> promise = new Promise<>();
                mRemote.transact(MSG_GET_PACKAGE_ARCHIVE_INFO, flags, 0, archiveFilePath, promise, 0);
                return Binder.get(promise);
            }

            @Override
            public ResolveInfo resolveService(Intent intent, int flags) throws RemoteException {
                Promise<ResolveInfo> promise = new Promise<>();
                mRemote.transact(MSG_RESOLVE_SERVICE, flags, 0, intent, promise, 0);
                return Binder.get(promise);
            }

            @Override
            public int checkPermission(String permissionName, int pid) throws RemoteException {
                Promise<Integer> promise = new Promise<>();
                mRemote.transact(MSG_CHECK_PERMISSION, pid, 0, permissionName, promise, 0);
                return Binder.get(promise);
            }

            @Override
            public String[] getPermissions(int pid) throws RemoteException {
                Promise<String[]> promise = new Promise<>();
                mRemote.transact(MSG_GET_PERMISSIONS, pid, 0, promise, 0);
                return Binder.get(promise);
            }

            @Override
            public void addListener(IPackageManagerListener listener) throws RemoteException {
                mRemote.transact(MSG_ADD_LISTENER, listener.asBinder(), null, FLAG_ONEWAY);
            }

            @Override
            public void removeListener(IPackageManagerListener listener) throws RemoteException {
                mRemote.transact(MSG_REMOVE_LISTENER, listener.asBinder(), null, FLAG_ONEWAY);
            }
        }

        private static class SmartProxy implements IPackageManager {
            private final IBinder mRemote;
            private final IPackageManager mStub;
            private final IPackageManager mProxy;

            SmartProxy(IBinder remote) {
                mRemote = remote;
                mStub = (IPackageManager) remote.queryLocalInterface(DESCRIPTOR);
                mProxy = new IPackageManager.Stub.Proxy(remote);
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
            public List<PackageInfo> getInstalledPackages(int flags) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    return mStub.getInstalledPackages(flags);
                } else {
                    return mProxy.getInstalledPackages(flags);
                }
            }

            @Override
            public PackageInfo getPackageInfo(String packageName, int flags) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    return mStub.getPackageInfo(packageName, flags);
                } else {
                    return mProxy.getPackageInfo(packageName, flags);
                }
            }

            @Override
            public PackageInfo getPackageArchiveInfo(String archiveFilePath, int flags) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    return mStub.getPackageArchiveInfo(archiveFilePath, flags);
                } else {
                    return mProxy.getPackageArchiveInfo(archiveFilePath, flags);
                }
            }

            @Override
            public ResolveInfo resolveService(Intent intent, int flags) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    return mStub.resolveService(intent, flags);
                } else {
                    return mProxy.resolveService(intent, flags);
                }
            }

            @Override
            public int checkPermission(String permissionName, int pid) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    return mStub.checkPermission(permissionName, pid);
                } else {
                    return mProxy.checkPermission(permissionName, pid);
                }
            }

            @Override
            public String[] getPermissions(int pid) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    return mStub.getPermissions(pid);
                } else {
                    return mProxy.getPermissions(pid);
                }
            }

            @Override
            public void addListener(IPackageManagerListener listener) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    mStub.addListener(IPackageManagerListener.Stub.asInterface(listener.asBinder()));
                } else {
                    mProxy.addListener(listener);
                }
            }

            @Override
            public void removeListener(IPackageManagerListener listener) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    mStub.removeListener(IPackageManagerListener.Stub.asInterface(listener.asBinder()));
                } else {
                    mProxy.removeListener(listener);
                }
            }
        }

        static final int MSG_GET_INSTALLED_PACKAGES = 1;
        static final int MSG_GET_PACKAGE_INFO = 2;
        static final int MSG_GET_PACKAGE_ARCHIVE_INFO = 3;
        static final int MSG_RESOLVE_SERVICE = 4;
        static final int MSG_CHECK_PERMISSION = 5;
        static final int MSG_GET_PERMISSIONS = 6;
        static final int MSG_ADD_LISTENER = 7;
        static final int MSG_REMOVE_LISTENER = 8;
    }

    public List<PackageInfo> getInstalledPackages(int flags) throws RemoteException;

    public PackageInfo getPackageInfo(String packageName, int flags) throws RemoteException;

    public PackageInfo getPackageArchiveInfo(String archiveFilePath, int flags) throws RemoteException;

    public ResolveInfo resolveService(Intent intent, int flags) throws RemoteException;

    public int checkPermission(String permissionName, int pid) throws RemoteException;

    public String[] getPermissions(int pid) throws RemoteException;

    public void addListener(IPackageManagerListener listener) throws RemoteException;

    public void removeListener(IPackageManagerListener listener) throws RemoteException;
}
