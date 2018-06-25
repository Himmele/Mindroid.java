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

package mindroid.runtime.inspection;

import mindroid.os.Bundle;
import mindroid.os.IInterface;
import mindroid.os.IBinder;
import mindroid.os.Binder;
import mindroid.os.RemoteException;
import mindroid.util.concurrent.Promise;

public interface ICommandHandler extends IInterface {
    public static abstract class Stub extends Binder implements ICommandHandler {
        private static final String DESCRIPTOR = "mindroid://interfaces/mindroid/runtime/inspection/ICommandHandler";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static ICommandHandler asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            return new ICommandHandler.Proxy(binder);
        }

        public IBinder asBinder() {
            return this;
        }

        @Override
        protected void onTransact(int what, int num, Object obj, Bundle data, Promise<?> result)
                throws RemoteException {
            switch (what) {
            case MSG_EXECUTE: {
                ((Promise<String>) result).completeWith(execute((String[]) obj));
                break;
            }
            default:
                super.onTransact(what, num, obj, data, result);
                break;
            }
        }

        private static class Proxy implements ICommandHandler {
            private final IBinder mRemote;

            Proxy(IBinder remote) {
                mRemote = remote;
            }

            public IBinder asBinder() {
                return mRemote;
            }

            public boolean equals(final Object obj) {
                if (obj == null)
                    return false;
                if (obj == this)
                    return true;
                if (obj instanceof Stub.Proxy) {
                    final Stub.Proxy that = (Stub.Proxy) obj;
                    return this.mRemote.equals(that.mRemote);
                }
                return false;
            }

            public int hashCode() {
                return mRemote.hashCode();
            }

            public Promise<String> execute(String[] arguments) throws RemoteException {
                Promise<String> promise = new Promise<>();
                mRemote.transact(MSG_EXECUTE, 0, arguments, null, promise, 0);
                return promise;
            }
        }

        static final int MSG_EXECUTE = 1;
    }

    static class Proxy implements ICommandHandler {
        private final IBinder mBinder;
        private final Stub mStub;
        private final ICommandHandler mProxy;

        Proxy(IBinder binder) {
            mBinder = binder;
            if (binder.getUri().getScheme().equals("mindroid")) {
                mStub = (Stub) binder.queryLocalInterface(Stub.DESCRIPTOR);
                mProxy = new Stub.Proxy(binder);
            } else {
                mindroid.runtime.system.Runtime runtime = mindroid.runtime.system.Runtime.getRuntime();
                mStub = (Stub) runtime.getBinder(binder.getId());
                mProxy = (ICommandHandler) runtime.getProxy(binder);
            }
        }

        public IBinder asBinder() {
            return mBinder;
        }

        public boolean equals(final Object obj) {
            if (obj == null)
                return false;
            if (obj == this)
                return true;
            if (obj instanceof Proxy) {
                final Proxy that = (Proxy) obj;
                return this.mBinder.equals(that.mBinder);
            }
            return false;
        }

        public int hashCode() {
            return mBinder.hashCode();
        }

        public Promise<String> execute(String[] arguments) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                return mStub.execute(arguments);
            } else {
                return mProxy.execute(arguments);
            }
        }
    }

    public Promise<String> execute(String[] arguments) throws RemoteException;
}
