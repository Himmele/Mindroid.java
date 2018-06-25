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

public interface IConsole extends IInterface {
    public static abstract class Stub extends Binder implements IConsole {
        private static final String DESCRIPTOR = "mindroid://interfaces/mindroid/runtime/inspection/IConsole";

        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }

        public static IConsole asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            return new IConsole.Proxy(binder);
        }

        public IBinder asBinder() {
            return this;
        }

        @Override
        protected void onTransact(int what, int num, Object obj, Bundle data, Promise<?> result) throws RemoteException {
            switch (what) {
            case MSG_ADD_COMMAND: {
                String command = data.getString("command");
                String description = data.getString("description");
                IBinder binder = data.getBinder("binder");
                ((Promise<Boolean>) result).complete(addCommand(command, description, ICommandHandler.Stub.asInterface(binder)));
                break;
            }
            case MSG_REMOVE_COMMAND: {
                ((Promise<Boolean>) result).complete(removeCommand((String) obj));
                break;
            }
            case MSG_EXECUTE_COMMAND: {
                String command = data.getString("command");
                String[] arguments = data.getStringArray("arguments");
                ((Promise<String>) result).completeWith(executeCommand(command, arguments));
                break;
            }
            default:
                super.onTransact(what, num, obj, data, result);
            }
        }

        private static class Proxy implements IConsole {
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
                if (obj instanceof Stub.Proxy) {
                    final Stub.Proxy that = (Stub.Proxy) obj;
                    return this.mRemote.equals(that.mRemote);
                }
                return false;
            }

            public int hashCode() {
                return mRemote.hashCode();
            }

            public boolean addCommand(String command, String description, ICommandHandler commandHandler) throws RemoteException {
                Bundle data = new Bundle();
                data.putString("command", command);
                data.putString("description", description);
                data.putBinder("binder", commandHandler.asBinder());
                Promise<Boolean> promise = new Promise<>();
                mRemote.transact(MSG_ADD_COMMAND, 0, null, data, promise, 0);
                return Binder.get(promise);
            }

            public boolean removeCommand(String command) throws RemoteException {
                Promise<Boolean> promise = new Promise<>();
                mRemote.transact(MSG_REMOVE_COMMAND, 0, command, null, promise, 0);
                return Binder.get(promise);
            }

            public Promise<String> executeCommand(String command, String[] arguments) throws RemoteException {
                Bundle data = new Bundle();
                data.putString("command", command);
                data.putStringArray("arguments", arguments);
                Promise<String> promise = new Promise<>();
                mRemote.transact(MSG_EXECUTE_COMMAND, 0, null, data, promise, 0);
                return promise;
            }
        }

        static final int MSG_ADD_COMMAND = 1;
        static final int MSG_REMOVE_COMMAND = 2;
        static final int MSG_EXECUTE_COMMAND = 3;
    }

    static class Proxy implements IConsole {
        private final IBinder mBinder;
        private final Stub mStub;
        private final IConsole mProxy;

        Proxy(IBinder binder) {
            mBinder = binder;
            if (binder.getUri().getScheme().equals("mindroid")) {
                mStub = (Stub) binder.queryLocalInterface(Stub.DESCRIPTOR);
                mProxy = new Stub.Proxy(binder);
            } else {
                mindroid.runtime.system.Runtime runtime = mindroid.runtime.system.Runtime.getRuntime();
                mStub = (Stub) runtime.getBinder(binder.getId());
                mProxy = (IConsole) runtime.getProxy(binder);
            }
        }

        public IBinder asBinder() {
            return mBinder;
        }

        public boolean equals(final Object obj) {
            if (obj == null) return false;
            if (obj == this) return true;
            if (obj instanceof Proxy) {
                final Proxy that = (Proxy) obj;
                return this.mBinder.equals(that.mBinder);
            }
            return false;
        }

        public int hashCode() {
            return mBinder.hashCode();
        }

        public boolean addCommand(String command, String description, ICommandHandler commandHandler) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                return mStub.addCommand(command, description, ICommandHandler.Stub.asInterface(commandHandler.asBinder()));
            } else {
                return mProxy.addCommand(command, description, commandHandler);
            }
        }

        public boolean removeCommand(String command) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                return mStub.removeCommand(command);
            } else {
                return mProxy.removeCommand(command);
            }
        }

        public Promise<String> executeCommand(String command, String[] arguments) throws RemoteException {
            if (mStub != null && mStub.isCurrentThread()) {
                return mStub.executeCommand(command, arguments);
            } else {
                return mProxy.executeCommand(command, arguments);
            }
        }
    }

    public boolean addCommand(String command, String description, ICommandHandler commandHandler) throws RemoteException;
    public boolean removeCommand(String command) throws RemoteException;
    public Promise<String> executeCommand(String command, String[] arguments) throws RemoteException;
}
