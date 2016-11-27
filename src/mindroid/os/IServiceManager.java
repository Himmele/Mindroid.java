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

public interface IServiceManager extends IInterface {
    public static abstract class Stub extends Binder implements IServiceManager {
        private static final java.lang.String DESCRIPTOR = "mindroid.os.IServiceManager";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static IServiceManager asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            return new IServiceManager.Stub.SmartProxy(binder);
        }

        public IBinder asBinder() {
            return this;
        }

        protected Object onTransact(int what, int arg1, int arg2, Object obj, Bundle data) throws RemoteException {
            switch (what) {
            case MSG_START_SERVICE: {
                Intent intent = (Intent) obj;
                ComponentName component = startService(intent);
                return component;
            }
            case MSG_STOP_SERVICE: {
                Intent intent = (Intent) obj;
                boolean result = stopService(intent);
                return Boolean.valueOf(result);
            }
            case MSG_BIND_SERVICE: {
                Intent intent = (Intent) data.getObject("intent");
                ServiceConnection conn = (ServiceConnection) data.getObject("conn");
                int flags = data.getInt("flags");
                IBinder binder = data.getBinder("binder");
                boolean result = bindService(intent, conn, flags, IRemoteCallback.Stub.asInterface(binder));
                return Boolean.valueOf(result);
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
                return null;
            }
            case MSG_START_SYSTEM_SERVICE: {
                Intent intent = (Intent) obj;
                ComponentName component = startSystemService(intent);
                return component;
            }
            case MSG_STOP_SYSTEM_SERVICE: {
                Intent intent = (Intent) obj;
                boolean result = stopSystemService(intent);
                return Boolean.valueOf(result);
            }
            default:
                return super.onTransact(what, arg1, arg2, obj, data);
            }
        }

        private static class Proxy implements IServiceManager {
            private final IBinder mRemote;

            Proxy(IBinder remote) {
                mRemote = remote;
            }

            public IBinder asBinder() {
                return mRemote;
            }

            public boolean equals(final Object obj) {
                if (obj == null) return false;
                if (obj == this) return true;
                if (obj instanceof Proxy) {
                    final Proxy that = (Proxy) obj;
                    return this.mRemote.equals(that.mRemote);
                }
                return false;
            }

            public int hashCode() {
                return mRemote.hashCode();
            }

            public ComponentName startService(Intent intent) throws RemoteException {
                return (ComponentName) mRemote.transact(MSG_START_SERVICE, intent, 0);
            }

            public boolean stopService(Intent intent) throws RemoteException {
                Boolean result = (Boolean) mRemote.transact(MSG_STOP_SERVICE, intent, 0);
                return result.booleanValue();
            }

            public boolean bindService(Intent intent, ServiceConnection conn, int flags, IRemoteCallback callback) throws RemoteException {
                Bundle data = new Bundle();
                data.putObject("intent", intent);
                data.putObject("conn", conn);
                data.putInt("flags", flags);
                data.putBinder("binder", callback.asBinder());
                Boolean result = (Boolean) mRemote.transact(MSG_BIND_SERVICE, data, 0);
                return result.booleanValue();
            }

            public void unbindService(Intent intent, ServiceConnection conn) throws RemoteException {
                Bundle data = new Bundle();
                data.putObject("intent", intent);
                data.putObject("conn", conn);
                mRemote.transact(MSG_UNBIND_SERVICE, data, FLAG_ONEWAY);
            }

            public void unbindService(Intent intent, ServiceConnection conn, IRemoteCallback callback) throws RemoteException {
                Bundle data = new Bundle();
                data.putObject("intent", intent);
                data.putObject("conn", conn);
                data.putBinder("binder", callback.asBinder());
                mRemote.transact(MSG_UNBIND_SERVICE, data, FLAG_ONEWAY);
            }

            public ComponentName startSystemService(Intent intent) throws RemoteException {
                ComponentName component = (ComponentName) mRemote.transact(MSG_START_SYSTEM_SERVICE, intent, 0);
                return component;
            }

            public boolean stopSystemService(Intent intent) throws RemoteException {
                Boolean result = (Boolean) mRemote.transact(MSG_STOP_SYSTEM_SERVICE, intent, 0);
                return result.booleanValue();
            }
        }

        private static class SmartProxy implements IServiceManager {
            private final IBinder mRemote;
            private final IServiceManager mStub;
            private final IServiceManager mProxy;

            SmartProxy(IBinder remote) {
                mRemote = remote;
                mStub = (IServiceManager) remote.queryLocalInterface(DESCRIPTOR);
                mProxy = new IServiceManager.Stub.Proxy(remote);
            }

            public IBinder asBinder() {
                return mRemote;
            }

            public boolean equals(final Object obj) {
                if (obj == null) return false;
                if (obj == this) return true;
                if (obj instanceof SmartProxy) {
                    final SmartProxy that = (SmartProxy) obj;
                    return this.mRemote.equals(that.mRemote);
                }
                return false;
            }

            public int hashCode() {
                return mRemote.hashCode();
            }

            public ComponentName startService(Intent intent) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    return mStub.startService(intent);
                } else {
                    return mProxy.startService(intent);
                }
            }

            public boolean stopService(Intent intent) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    return mStub.stopService(intent);
                } else {
                    return mProxy.stopService(intent);
                }
            }

            public boolean bindService(Intent intent, ServiceConnection conn, int flags, IRemoteCallback callback) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    return mStub.bindService(intent, conn, flags, IRemoteCallback.Stub.asInterface(callback.asBinder()));
                } else {
                    return mProxy.bindService(intent, conn, flags, callback);
                }
            }

            public void unbindService(Intent intent, ServiceConnection conn) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    mStub.unbindService(intent, conn);
                } else {
                    mProxy.unbindService(intent, conn);
                }
            }

            public void unbindService(Intent intent, ServiceConnection conn, IRemoteCallback callback) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    mStub.unbindService(intent, conn, IRemoteCallback.Stub.asInterface(callback.asBinder()));
                } else {
                    mProxy.unbindService(intent, conn, callback);
                }
            }

            public ComponentName startSystemService(Intent intent) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    return mStub.startSystemService(intent);
                } else {
                    return mProxy.startSystemService(intent);
                }
            }

            public boolean stopSystemService(Intent intent) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    return mStub.stopSystemService(intent);
                } else {
                    return mProxy.stopSystemService(intent);
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

    public ComponentName startService(Intent intent) throws RemoteException;

    public boolean stopService(Intent intent) throws RemoteException;

    public boolean bindService(Intent intent, ServiceConnection conn, int flags, IRemoteCallback callback) throws RemoteException;

    public void unbindService(Intent intent, ServiceConnection conn) throws RemoteException;

    public void unbindService(Intent intent, ServiceConnection conn, IRemoteCallback callback) throws RemoteException;

    public ComponentName startSystemService(Intent intent) throws RemoteException;

    public boolean stopSystemService(Intent intent) throws RemoteException;
}
