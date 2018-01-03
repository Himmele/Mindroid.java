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

import mindroid.content.Intent;
import mindroid.content.ServiceConnection;
import mindroid.util.concurrent.Promise;

public interface IProcess extends IInterface {
    public static abstract class Stub extends Binder implements IProcess {
        private static final java.lang.String DESCRIPTOR = "mindroid.os.IProcess";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static IProcess asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            return new IProcess.Stub.SmartProxy(binder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        protected void onTransact(int what, int arg1, int arg2, Object obj, Bundle data, Promise<?> result) throws RemoteException {
            switch (what) {
            case MSG_CREATE_SERVICE: {
                Intent intent = (Intent) data.getObject("intent");
                IBinder binder = data.getBinder("binder");
                createService(intent, IRemoteCallback.Stub.asInterface(binder));
                break;
            }
            case MSG_START_SERVICE: {
                Intent intent = (Intent) data.getObject("intent");
                int flags = arg1;
                int startId = arg2;
                IBinder binder = data.getBinder("binder");
                startService(intent, flags, startId, IRemoteCallback.Stub.asInterface(binder));
                break;
            }
            case MSG_STOP_SERVICE: {
                Intent intent = (Intent) data.getObject("intent");
                IBinder binder = data.getBinder("binder");
                if (binder == null) {
                    stopService(intent);
                } else {
                    stopService(intent, IRemoteCallback.Stub.asInterface(binder));
                }
                break;
            }
            case MSG_BIND_SERVICE: {
                Intent intent = (Intent) data.getObject("intent");
                ServiceConnection conn = (ServiceConnection) data.getObject("conn");
                int flags = data.getInt("flags");
                IBinder binder = data.getBinder("binder");
                bindService(intent, conn, flags, IRemoteCallback.Stub.asInterface(binder));
                break;
            }
            case MSG_UNBIND_SERVICE: {
                Intent intent = (Intent) data.getObject("intent");
                IBinder binder = data.getBinder("binder");
                if (binder == null) {
                    unbindService(intent);
                } else {
                    unbindService(intent, IRemoteCallback.Stub.asInterface(binder));
                }
                break;
            }
            default:
                super.onTransact(what, arg1, arg2, obj, data, result);
                break;
            }
        }

        private static class Proxy implements IProcess {
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
            public void createService(Intent intent, IRemoteCallback callback) throws RemoteException {
                Bundle data = new Bundle();
                data.putObject("intent", intent);
                data.putBinder("binder", callback.asBinder());
                mRemote.transact(MSG_CREATE_SERVICE, data, null, FLAG_ONEWAY);
            }

            @Override
            public void startService(Intent intent, int flags, int startId, IRemoteCallback callback) throws RemoteException {
                Bundle data = new Bundle();
                data.putObject("intent", intent);
                data.putBinder("binder", callback.asBinder());
                mRemote.transact(MSG_START_SERVICE, flags, startId, data, null, FLAG_ONEWAY);
            }

            @Override
            public void stopService(Intent intent) throws RemoteException {
                Bundle data = new Bundle();
                data.putObject("intent", intent);
                mRemote.transact(MSG_STOP_SERVICE, data, null, FLAG_ONEWAY);
            }

            @Override
            public void stopService(Intent intent, IRemoteCallback callback) throws RemoteException {
                Bundle data = new Bundle();
                data.putObject("intent", intent);
                data.putBinder("binder", callback.asBinder());
                mRemote.transact(MSG_STOP_SERVICE, data, null, FLAG_ONEWAY);
            }

            @Override
            public void bindService(Intent intent, ServiceConnection conn, int flags, IRemoteCallback callback) throws RemoteException {
                Bundle data = new Bundle();
                data.putObject("intent", intent);
                data.putObject("conn", conn);
                data.putInt("flags", flags);
                data.putBinder("binder", callback.asBinder());
                mRemote.transact(MSG_BIND_SERVICE, data, null, FLAG_ONEWAY);
            }

            @Override
            public void unbindService(Intent intent) throws RemoteException {
                Bundle data = new Bundle();
                data.putObject("intent", intent);
                mRemote.transact(MSG_UNBIND_SERVICE, data, null, FLAG_ONEWAY);
            }

            @Override
            public void unbindService(Intent intent, IRemoteCallback callback) throws RemoteException {
                Bundle data = new Bundle();
                data.putObject("intent", intent);
                data.putBinder("binder", callback.asBinder());
                mRemote.transact(MSG_UNBIND_SERVICE, data, null, FLAG_ONEWAY);
            }
        }

        private static class SmartProxy implements IProcess {
            private final IBinder mRemote;
            private final IProcess mStub;
            private final IProcess mProxy;

            SmartProxy(IBinder remote) {
                mRemote = remote;
                mStub = (IProcess) remote.queryLocalInterface(DESCRIPTOR);
                mProxy = new IProcess.Stub.Proxy(remote);
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
            public void createService(Intent intent, IRemoteCallback callback) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    mStub.createService(intent, IRemoteCallback.Stub.asInterface(callback.asBinder()));
                } else {
                    mProxy.createService(intent, callback);
                }
            }

            @Override
            public void startService(Intent intent, int flags, int startId, IRemoteCallback callback) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    mStub.startService(intent, flags, startId, IRemoteCallback.Stub.asInterface(callback.asBinder()));
                } else {
                    mProxy.startService(intent, flags, startId, callback);
                }
            }

            @Override
            public void stopService(Intent intent) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    mStub.stopService(intent);
                } else {
                    mProxy.stopService(intent);
                }
            }

            @Override
            public void stopService(Intent intent, IRemoteCallback callback) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    mStub.stopService(intent, IRemoteCallback.Stub.asInterface(callback.asBinder()));
                } else {
                    mProxy.stopService(intent, callback);
                }
            }

            @Override
            public void bindService(Intent intent, ServiceConnection conn, int flags, IRemoteCallback callback) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    mStub.bindService(intent, conn, flags, IRemoteCallback.Stub.asInterface(callback.asBinder()));
                } else {
                    mProxy.bindService(intent, conn, flags, callback);
                }
            }

            @Override
            public void unbindService(Intent intent) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    mStub.unbindService(intent);
                } else {
                    mProxy.unbindService(intent);
                }
            }

            @Override
            public void unbindService(Intent intent, IRemoteCallback callback) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    mStub.unbindService(intent, IRemoteCallback.Stub.asInterface(callback.asBinder()));
                } else {
                    mProxy.unbindService(intent, callback);
                }
            }
        }

        static final int MSG_CREATE_SERVICE = 1;
        static final int MSG_START_SERVICE = 2;
        static final int MSG_STOP_SERVICE = 3;
        static final int MSG_BIND_SERVICE = 4;
        static final int MSG_UNBIND_SERVICE = 5;
    }

    public void createService(Intent intent, IRemoteCallback callback) throws RemoteException;

    public void startService(Intent intent, int flags, int startId, IRemoteCallback callback) throws RemoteException;

    public void stopService(Intent intent) throws RemoteException;

    public void stopService(Intent intent, IRemoteCallback callback) throws RemoteException;

    public void bindService(Intent intent, ServiceConnection conn, int flags, IRemoteCallback callback) throws RemoteException;

    public void unbindService(Intent intent) throws RemoteException;

    public void unbindService(Intent intent, IRemoteCallback callback) throws RemoteException;
}
