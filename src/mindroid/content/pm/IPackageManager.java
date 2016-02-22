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

		public IBinder asBinder() {
			return this;
		}

		protected Object onTransact(int what, int arg1, int arg2, Object obj, Bundle data) throws RemoteException {
			switch (what) {
			case MSG_RESOLVE_SERVICE: {
				Intent intent = (Intent) obj;
				ResolveInfo resolveInfo = resolveService(intent, arg1);
				return resolveInfo;
			}
			case MSG_ADD_LISTENER: {
				addListener(IPackageManagerListener.Stub.asInterface((IBinder) obj));
				return null;
			}
			case MSG_REMOVE_LISTENER: {
				removeListener(IPackageManagerListener.Stub.asInterface((IBinder) obj));
				return null;
			}
			case MSG_GET_INSTALLED_PACKAGES: {
				List packages = getInstalledPackages(arg1);
				return packages;
			}
			default:
				return super.onTransact(what, arg1, arg2, obj, data);
			}
		}

		private static class Proxy implements IPackageManager {
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

			public ResolveInfo resolveService(Intent intent, int flags) throws RemoteException {
				ResolveInfo resolveInfo = (ResolveInfo) mRemote.transact(MSG_RESOLVE_SERVICE, flags, 0, intent, 0);
				return resolveInfo;
			}

			public void addListener(IPackageManagerListener listener) throws RemoteException {
				mRemote.transact(MSG_ADD_LISTENER, listener.asBinder(), FLAG_ONEWAY);
			}

			public void removeListener(IPackageManagerListener listener) throws RemoteException {
				mRemote.transact(MSG_REMOVE_LISTENER, listener.asBinder(), FLAG_ONEWAY);
			}

			public List getInstalledPackages(int flags) throws RemoteException {
				List packages = (List) mRemote.transact(MSG_GET_INSTALLED_PACKAGES, flags, 0, 0);
				return packages;
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

			public List getInstalledPackages(int flags) throws RemoteException {
				if (mRemote.runsOnSameThread()) {
					return mStub.getInstalledPackages(flags);
				} else {
					return mProxy.getInstalledPackages(flags);
				}
			}

			public ResolveInfo resolveService(Intent intent, int flags) throws RemoteException {
				if (mRemote.runsOnSameThread()) {
					return mStub.resolveService(intent, flags);
				} else {
					return mProxy.resolveService(intent, flags);
				}
			}

			public void addListener(IPackageManagerListener listener) throws RemoteException {
				if (mRemote.runsOnSameThread()) {
					mStub.addListener(IPackageManagerListener.Stub.asInterface(listener.asBinder()));
				} else {
					mProxy.addListener(listener);
				}
			}

			public void removeListener(IPackageManagerListener listener) throws RemoteException {
				if (mRemote.runsOnSameThread()) {
					mStub.removeListener(IPackageManagerListener.Stub.asInterface(listener.asBinder()));
				} else {
					mProxy.removeListener(listener);
				}
			}
		}

		static final int MSG_GET_INSTALLED_PACKAGES = 1;
		static final int MSG_RESOLVE_SERVICE = 2;
		static final int MSG_ADD_LISTENER = 3;
		static final int MSG_REMOVE_LISTENER = 4;
	}

	public List getInstalledPackages(int flags) throws RemoteException;

	public ResolveInfo resolveService(Intent intent, int flags) throws RemoteException;

	public void addListener(IPackageManagerListener listener) throws RemoteException;

	public void removeListener(IPackageManagerListener listener) throws RemoteException;
}
