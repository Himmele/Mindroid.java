/*
 * Copyright (C) 2020 E.S.R.Labs
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

package mindroid.runtime.sd;

import mindroid.os.Binder;
import mindroid.os.Bundle;
import mindroid.os.IBinder;
import mindroid.os.IInterface;
import mindroid.os.Looper;
import mindroid.os.RemoteException;
import mindroid.util.concurrent.Promise;

import java.util.concurrent.Executor;

public interface IDiscoveryListener extends IInterface {
    public static abstract class Stub extends Binder implements IDiscoveryListener {
        public static final String DESCRIPTOR = "mindroid://interfaces/mindroid/runtime/sd/IDiscoveryListener";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public Stub(Looper looper) {
            super(looper);
            this.attachInterface(this, DESCRIPTOR);
        }

        public Stub(Executor executor) {
            super(executor);
            this.attachInterface(this, DESCRIPTOR);
        }

        public static IDiscoveryListener asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            return new IDiscoveryListener.Proxy(binder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        protected void onTransact(int what, int num, Object obj, Bundle data, Promise<?> result) throws RemoteException {
            switch (what) {
            case MSG_ON_DISCOVERY_STARTED: {
                onDiscoveryStarted();
                break;
            }
            case MSG_ON_DISCOVERY_STOPPED: {
                onDiscoveryStopped();
                break;
            }
            case MSG_ON_SERVICE_FOUND: {
                onServiceFound((ServiceInfo) obj);
                break;
            }
            case MSG_ON_SERVICE_LOST: {
                onServiceLost((ServiceInfo) obj);
                break;
            }
            case MSG_ON_START_DISCOVERY_FAILED: {
                onStartDiscoveryFailed((Throwable) obj);
                break;
            }
            case MSG_ON_STOP_DISCOVERY_FAILED: {
                onStopDiscoveryFailed((Throwable) obj);
                break;
            }
            default:
                super.onTransact(what, num, obj, data, result);
                break;
            }
        }

        private static class Proxy implements IDiscoveryListener {
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
            public void onDiscoveryStarted() throws RemoteException {
                mRemote.transact(MSG_ON_DISCOVERY_STARTED, 0, null, null, null, FLAG_ONEWAY);
            }

            @Override
            public void onDiscoveryStopped() throws RemoteException {
                mRemote.transact(MSG_ON_DISCOVERY_STOPPED, 0, null, null, null, FLAG_ONEWAY);
            }

            @Override
            public void onServiceFound(ServiceInfo service) throws RemoteException {
                mRemote.transact(MSG_ON_SERVICE_FOUND, 0, service, null, null, FLAG_ONEWAY);
            }

            @Override
            public void onServiceLost(ServiceInfo service) throws RemoteException {
                mRemote.transact(MSG_ON_SERVICE_LOST, 0, service, null, null, FLAG_ONEWAY);
            }

            @Override
            public void onStartDiscoveryFailed(Throwable cause) throws RemoteException {
                mRemote.transact(MSG_ON_START_DISCOVERY_FAILED, 0, cause, null, null, FLAG_ONEWAY);
            }

            @Override
            public void onStopDiscoveryFailed(Throwable cause) throws RemoteException {
                mRemote.transact(MSG_ON_STOP_DISCOVERY_FAILED, 0, cause, null, null, FLAG_ONEWAY);
            }
        }

        static final int MSG_ON_DISCOVERY_STARTED = 1;
        static final int MSG_ON_DISCOVERY_STOPPED = 2;
        static final int MSG_ON_SERVICE_FOUND = 3;
        static final int MSG_ON_SERVICE_LOST = 4;
        static final int MSG_ON_START_DISCOVERY_FAILED = 5;
        static final int MSG_ON_STOP_DISCOVERY_FAILED = 6;
    }

    static class Proxy implements IDiscoveryListener {
        private final IBinder mBinder;
        private final Stub mStub;
        private final IDiscoveryListener mProxy;

        Proxy(IBinder binder) {
            mBinder = binder;
            if (binder.getUri().getScheme().equals("mindroid")) {
                mStub = (IDiscoveryListener.Stub) binder.queryLocalInterface(IDiscoveryListener.Stub.DESCRIPTOR);
                mProxy = new IDiscoveryListener.Stub.Proxy(binder);
            } else {
                mindroid.runtime.system.Runtime runtime = mindroid.runtime.system.Runtime.getRuntime();
                mStub = (IDiscoveryListener.Stub) runtime.getBinder(binder.getId());
                mProxy = (IDiscoveryListener) runtime.getProxy(binder);
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
        public void onDiscoveryStarted() throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                mStub.onDiscoveryStarted();
            } else {
                mProxy.onDiscoveryStarted();
            }
        }

        @Override
        public void onDiscoveryStopped() throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                mStub.onDiscoveryStopped();
            } else {
                mProxy.onDiscoveryStopped();
            }
        }

        @Override
        public void onServiceFound(ServiceInfo service) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                mStub.onServiceFound(service);
            } else {
                mProxy.onServiceFound(service);
            }
        }

        @Override
        public void onServiceLost(ServiceInfo service) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                mStub.onServiceLost(service);
            } else {
                mProxy.onServiceLost(service);
            }
        }

        @Override
        public void onStartDiscoveryFailed(Throwable cause) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                mStub.onStartDiscoveryFailed(cause);
            } else {
                mProxy.onStartDiscoveryFailed(cause);
            }
        }

        @Override
        public void onStopDiscoveryFailed(Throwable cause) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                mStub.onStopDiscoveryFailed(cause);
            } else {
                mProxy.onStopDiscoveryFailed(cause);
            }
        }
    }

    public void onDiscoveryStarted() throws RemoteException;
    public void onDiscoveryStopped() throws RemoteException;
    public void onServiceFound(ServiceInfo service) throws RemoteException;
    public void onServiceLost(ServiceInfo service) throws RemoteException;
    public void onStartDiscoveryFailed(Throwable cause) throws RemoteException;
    public void onStopDiscoveryFailed(Throwable cause) throws RemoteException;
}
