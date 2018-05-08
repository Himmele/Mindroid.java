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

package mindroid.os;

import mindroid.content.ComponentName;
import mindroid.content.Intent;
import mindroid.content.ServiceConnection;
import mindroid.os.Binder;
import mindroid.os.IBinder;
import mindroid.os.IInterface;
import mindroid.util.concurrent.Promise;

public interface IServiceManager extends IInterface {
    public static abstract class Stub extends Binder implements IServiceManager {
        private static final String DESCRIPTOR = "mindroid://interfaces/mindroid/os/IServiceManager";

        public Stub(Looper looper) {
            super(looper);
            this.attachInterface(this, DESCRIPTOR);
        }

        public static IServiceManager asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            return new IServiceManager.Proxy(binder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        protected void onTransact(int what, int num, Object obj, Bundle data, Promise<?> result) throws RemoteException {
            switch (what) {
            case MSG_START_SERVICE: {
                Intent intent = (Intent) obj;
                ComponentName component = startService(intent);
                ((Promise<ComponentName>) result).complete(component);
                break;
            }
            case MSG_STOP_SERVICE: {
                Intent intent = (Intent) obj;
                ((Promise<Boolean>) result).complete(stopService(intent));
                break;
            }
            case MSG_BIND_SERVICE: {
                Intent intent = (Intent) data.getObject("intent");
                ServiceConnection conn = (ServiceConnection) data.getObject("conn");
                int flags = data.getInt("flags");
                IBinder binder = data.getBinder("binder");
                ((Promise<Boolean>) result).complete(bindService(intent, conn, flags, IRemoteCallback.Stub.asInterface(binder)));
                break;
            }
            case MSG_UNBIND_SERVICE: {
                Intent intent = (Intent) data.getObject("intent");
                ServiceConnection conn = (ServiceConnection) data.getObject("conn");
                IBinder binder = data.getBinder("binder");
                if (binder == null) {
                    unbindService(intent, conn);
                } else {
                    unbindService(intent, conn, IRemoteCallback.Stub.asInterface(binder));
                }
                break;
            }
            case MSG_START_SYSTEM_SERVICE: {
                Intent intent = (Intent) obj;
                ComponentName component = startSystemService(intent);
                ((Promise<ComponentName>) result).complete(component);
                break;
            }
            case MSG_STOP_SYSTEM_SERVICE: {
                Intent intent = (Intent) obj;
                ((Promise<Boolean>) result).complete(stopSystemService(intent));
                break;
            }
            default:
                super.onTransact(what, num, obj, data, result);
                break;
            }
        }

        private static class Proxy implements IServiceManager {
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
            public ComponentName startService(Intent intent) throws RemoteException {
                Promise<ComponentName> promise = new Promise<>();
                mRemote.transact(MSG_START_SERVICE, 0, intent, null, promise, 0);
                return Binder.get(promise);
            }

            @Override
            public boolean stopService(Intent intent) throws RemoteException {
                Promise<Boolean> promise = new Promise<>();
                mRemote.transact(MSG_STOP_SERVICE, 0, intent, null, promise, 0);
                return Binder.get(promise);
            }

            @Override
            public boolean bindService(Intent intent, ServiceConnection conn, int flags, IRemoteCallback callback) throws RemoteException {
                Bundle data = new Bundle();
                data.putObject("intent", intent);
                data.putObject("conn", conn);
                data.putInt("flags", flags);
                data.putBinder("binder", callback.asBinder());
                Promise<Boolean> promise = new Promise<>();
                mRemote.transact(MSG_BIND_SERVICE, 0, null, data, promise, 0);
                return Binder.get(promise);
            }

            @Override
            public void unbindService(Intent intent, ServiceConnection conn) throws RemoteException {
                Bundle data = new Bundle();
                data.putObject("intent", intent);
                data.putObject("conn", conn);
                mRemote.transact(MSG_UNBIND_SERVICE, 0, null, data, null, FLAG_ONEWAY);
            }

            @Override
            public void unbindService(Intent intent, ServiceConnection conn, IRemoteCallback callback) throws RemoteException {
                Bundle data = new Bundle();
                data.putObject("intent", intent);
                data.putObject("conn", conn);
                data.putBinder("binder", callback.asBinder());
                mRemote.transact(MSG_UNBIND_SERVICE, 0, null, data, null, FLAG_ONEWAY);
            }

            @Override
            public ComponentName startSystemService(Intent intent) throws RemoteException {
                final long timeout = intent.getLongExtra("timeout", -1);
                Promise<ComponentName> promise = new Promise<>();
                mRemote.transact(MSG_START_SYSTEM_SERVICE, 0, intent, null, promise, 0);
                if (timeout < 0) {
                    return Binder.get(promise);
                } else {
                    return Binder.get(promise, timeout);
                }
            }

            @Override
            public boolean stopSystemService(Intent intent) throws RemoteException {
                final long timeout = intent.getLongExtra("timeout", -1);
                Promise<Boolean> promise = new Promise<>();
                mRemote.transact(MSG_STOP_SYSTEM_SERVICE, 0, intent, null, promise, 0);
                if (timeout < 0) {
                    return Binder.get(promise);
                } else {
                    return Binder.get(promise, timeout);
                }
            }
        }

        static final int MSG_START_SERVICE = 1;
        static final int MSG_STOP_SERVICE = 2;
        static final int MSG_BIND_SERVICE = 3;
        static final int MSG_UNBIND_SERVICE = 4;
        static final int MSG_START_SYSTEM_SERVICE = 5;
        static final int MSG_STOP_SYSTEM_SERVICE = 6;
    }

    static class Proxy implements IServiceManager {
        private final IBinder mBinder;
        private final Stub mStub;
        private final IServiceManager mProxy;

        Proxy(IBinder binder) {
            mBinder = binder;
            if (binder.getUri().getScheme().equals("mindroid")) {
                mStub = (Stub) binder.queryLocalInterface(Stub.DESCRIPTOR);
                mProxy = new Stub.Proxy(binder);
            } else {
                mindroid.runtime.system.Runtime runtime = mindroid.runtime.system.Runtime.getRuntime();
                mStub = (Stub) runtime.getBinder(binder.getId());
                mProxy = (IServiceManager) runtime.getProxy(binder);
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
        public ComponentName startService(Intent intent) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                return mStub.startService(intent);
            } else {
                return mProxy.startService(intent);
            }
        }

        @Override
        public boolean stopService(Intent intent) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                return mStub.stopService(intent);
            } else {
                return mProxy.stopService(intent);
            }
        }

        @Override
        public boolean bindService(Intent intent, ServiceConnection conn, int flags, IRemoteCallback callback) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                return mStub.bindService(intent, conn, flags, IRemoteCallback.Stub.asInterface(callback.asBinder()));
            } else {
                return mProxy.bindService(intent, conn, flags, callback);
            }
        }

        @Override
        public void unbindService(Intent intent, ServiceConnection conn) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                mStub.unbindService(intent, conn);
            } else {
                mProxy.unbindService(intent, conn);
            }
        }

        @Override
        public void unbindService(Intent intent, ServiceConnection conn, IRemoteCallback callback) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                mStub.unbindService(intent, conn, IRemoteCallback.Stub.asInterface(callback.asBinder()));
            } else {
                mProxy.unbindService(intent, conn, callback);
            }
        }

        @Override
        public ComponentName startSystemService(Intent intent) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                return mStub.startSystemService(intent);
            } else {
                return mProxy.startSystemService(intent);
            }
        }

        @Override
        public boolean stopSystemService(Intent intent) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                return mStub.stopSystemService(intent);
            } else {
                return mProxy.stopSystemService(intent);
            }
        }
    }

    public ComponentName startService(Intent intent) throws RemoteException;
    public boolean stopService(Intent intent) throws RemoteException;
    public boolean bindService(Intent intent, ServiceConnection conn, int flags, IRemoteCallback callback) throws RemoteException;
    public void unbindService(Intent intent, ServiceConnection conn) throws RemoteException;
    public void unbindService(Intent intent, ServiceConnection conn, IRemoteCallback callback) throws RemoteException;
    public ComponentName startSystemService(Intent intent) throws RemoteException;
    public boolean stopSystemService(Intent intent) throws RemoteException;
}
