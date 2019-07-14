/*
 * Copyright (C) 2018 E.S.R.Labs
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

package mindroid.runtime.system;

import java.net.URI;
import mindroid.os.Binder;
import mindroid.os.Bundle;
import mindroid.os.IBinder;
import mindroid.os.IInterface;
import mindroid.os.Parcel;
import mindroid.os.RemoteException;
import mindroid.util.concurrent.Promise;

public abstract class Plugin {
    protected Runtime mRuntime;

    public void setUp(Runtime runtime) {
        mRuntime = runtime;
    }

    public void tearDown() {
        mRuntime = null;
    }

    public abstract void start();
    public abstract void stop();

    public abstract void attachBinder(Binder binder);
    public abstract void detachBinder(long id);
    public abstract void attachProxy(long proxyId, Binder.Proxy proxy);
    public abstract void detachProxy(long proxyId, long binderId);

    public abstract Binder getStub(Binder binder);
    public abstract IInterface getProxy(IBinder binder);
    public abstract Promise<Void> connect(URI node, Bundle extras);
    public abstract Promise<Void> disconnect(URI node, Bundle extras);
    public abstract Promise<Parcel> transact(IBinder binder, int what, Parcel data, int flags) throws RemoteException;

    public abstract void link(IBinder binder, IBinder.Supervisor supervisor, Bundle extras) throws RemoteException;
    public abstract boolean unlink(IBinder binder, IBinder.Supervisor supervisor, Bundle extras);

    public static abstract class Observer {
        public abstract void onEntry(int nodeId);
        public abstract void onExit(int nodeId);
    }
}
