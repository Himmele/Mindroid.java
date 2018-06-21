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
            return new IServiceManager.Stub.Proxy(binder);
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
                ((Promise<ComponentName>) result).completeWith(startService(intent));
                break;
            }
            case MSG_STOP_SERVICE: {
                Intent intent = (Intent) obj;
                ((Promise<Boolean>) result).completeWith(stopService(intent));
                break;
            }
            case MSG_BIND_SERVICE: {
                Intent intent = (Intent) data.getObject("intent");
                ServiceConnection conn = (ServiceConnection) data.getObject("conn");
                int flags = data.getInt("flags");
                IBinder binder = data.getBinder("binder");
                ((Promise<Boolean>) result).completeWith(bindService(intent, conn, flags, IRemoteCallback.Stub.asInterface(binder)));
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
                ((Promise<ComponentName>) result).completeWith(startSystemService(intent));
                break;
            }
            case MSG_STOP_SYSTEM_SERVICE: {
                Intent intent = (Intent) obj;
                ((Promise<Boolean>) result).completeWith(stopSystemService(intent));
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
            public Promise<ComponentName> startService(Intent intent) throws RemoteException {
                Promise<ComponentName> promise = new Promise<>();
                mRemote.transact(MSG_START_SERVICE, 0, intent, null, promise, 0);
                return promise;
            }

            @Override
            public Promise<Boolean> stopService(Intent intent) throws RemoteException {
                Promise<Boolean> promise = new Promise<>();
                mRemote.transact(MSG_STOP_SERVICE, 0, intent, null, promise, 0);
                return promise;
            }

            @Override
            public Promise<Boolean> bindService(Intent intent, ServiceConnection conn, int flags, IRemoteCallback callback) throws RemoteException {
                Bundle data = new Bundle();
                data.putObject("intent", intent);
                data.putObject("conn", conn);
                data.putInt("flags", flags);
                data.putBinder("binder", callback.asBinder());
                Promise<Boolean> promise = new Promise<>();
                mRemote.transact(MSG_BIND_SERVICE, 0, null, data, promise, 0);
                return promise;
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
            public Promise<ComponentName> startSystemService(Intent intent) throws RemoteException {
                Promise<ComponentName> promise = new Promise<>();
                mRemote.transact(MSG_START_SYSTEM_SERVICE, 0, intent, null, promise, 0);
                return promise;
            }

            @Override
            public Promise<Boolean> stopSystemService(Intent intent) throws RemoteException {
                Promise<Boolean> promise = new Promise<>();
                mRemote.transact(MSG_STOP_SYSTEM_SERVICE, 0, intent, null, promise, 0);
                return promise;
            }
        }

        static final int MSG_START_SERVICE = 1;
        static final int MSG_STOP_SERVICE = 2;
        static final int MSG_BIND_SERVICE = 3;
        static final int MSG_UNBIND_SERVICE = 4;
        static final int MSG_START_SYSTEM_SERVICE = 5;
        static final int MSG_STOP_SYSTEM_SERVICE = 6;
    }

    public Promise<ComponentName> startService(Intent intent) throws RemoteException;
    public Promise<Boolean> stopService(Intent intent) throws RemoteException;
    public Promise<Boolean> bindService(Intent intent, ServiceConnection conn, int flags, IRemoteCallback callback) throws RemoteException;
    public void unbindService(Intent intent, ServiceConnection conn) throws RemoteException;
    public void unbindService(Intent intent, ServiceConnection conn, IRemoteCallback callback) throws RemoteException;
    public Promise<ComponentName> startSystemService(Intent intent) throws RemoteException;
    public Promise<Boolean> stopSystemService(Intent intent) throws RemoteException;
}
