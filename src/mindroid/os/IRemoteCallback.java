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

public interface IRemoteCallback extends IInterface {
    public static abstract class Stub extends Binder implements IRemoteCallback {
        private static final java.lang.String DESCRIPTOR = "mindroid.os.IRemoteCallback";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static IRemoteCallback asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            return new IRemoteCallback.Stub.SmartProxy(binder);
        }

        public IBinder asBinder() {
            return this;
        }

        protected Object onTransact(int what, int arg1, int arg2, Object obj, Bundle data) throws RemoteException {
            switch (what) {
            case MSG_SEND_RESULT: {
                sendResult(data);
                return null;
            }
            default:
                return super.onTransact(what, arg1, arg2, obj, data);
            }
        }

        private static class Proxy implements IRemoteCallback {
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

            public void sendResult(Bundle data) throws RemoteException {
                mRemote.transact(MSG_SEND_RESULT, data, FLAG_ONEWAY);
            }
        }

        private static class SmartProxy implements IRemoteCallback {
            private final IBinder mRemote;
            private final IRemoteCallback mStub;
            private final IRemoteCallback mProxy;

            SmartProxy(IBinder remote) {
                mRemote = remote;
                mStub = (IRemoteCallback) remote.queryLocalInterface(DESCRIPTOR);
                mProxy = new IRemoteCallback.Stub.Proxy(remote);
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

            public void sendResult(Bundle data) throws RemoteException {
                if (mRemote.runsOnSameThread()) {
                    mStub.sendResult(data);
                } else {
                    mProxy.sendResult(data);
                }
            }
        }

        static final int MSG_SEND_RESULT = 1;
    }

    public void sendResult(Bundle data) throws RemoteException;
}
