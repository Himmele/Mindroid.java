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

import mindroid.content.ComponentName;
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
			if(binder == null) {
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
				Intent service = (Intent) obj;
				ServiceInfo serviceInfo = resolveService(service);
				return serviceInfo;
			}
			case MSG_ADD_LISTENER: {
				addListener(IPackageManagerListener.Stub.asInterface((IBinder) obj));
				return null;
			}
			case MSG_REMOVE_LISTENER: {
				removeListener(IPackageManagerListener.Stub.asInterface((IBinder) obj));
				return null;
			}
			case MSG_GET_AUTOSTART_SERVICES: {
				ComponentName[] components = getAutostartServices();
				return components;
			}
			default:
			    return super.onTransact(what, arg1, arg2, obj, data);
			}
		}
		
		private static class Proxy implements IPackageManager {
			private final IBinder mBinder;
			
			Proxy(IBinder binder) {
				mBinder = binder;
			}
			
			public IBinder asBinder() {
				return mBinder;
			}
			
			public ServiceInfo resolveService(Intent service) throws RemoteException {
				ServiceInfo serviceInfo = (ServiceInfo) mBinder.transact(MSG_RESOLVE_SERVICE, service, 0);
				return serviceInfo;
			}

			public void addListener(IPackageManagerListener listener) throws RemoteException {
				mBinder.transact(MSG_ADD_LISTENER, listener.asBinder(), FLAG_ONEWAY);
			}

			public void removeListener(IPackageManagerListener listener) throws RemoteException {
				mBinder.transact(MSG_REMOVE_LISTENER, listener.asBinder(), FLAG_ONEWAY);
			}

			public ComponentName[] getAutostartServices() throws RemoteException {
				ComponentName[] components = (ComponentName[]) mBinder.transact(MSG_GET_AUTOSTART_SERVICES, 0);
				return components;
			}
		}
		
		private static class SmartProxy implements IPackageManager {
			private final IBinder mBinder;
			private final IPackageManager mStub;
			private final IPackageManager mProxy;
			
			SmartProxy(IBinder binder) {
				mBinder = binder;
				mStub = (IPackageManager) binder.queryLocalInterface(DESCRIPTOR);
				mProxy = new IPackageManager.Stub.Proxy(binder);
			}
			
			public IBinder asBinder() {
				return mBinder;
			}
			
			public ServiceInfo resolveService(Intent service) throws RemoteException {
				if (mBinder.runsOnSameThread()) {
					return mStub.resolveService(service);
				} else {
					return mProxy.resolveService(service);
				}
			}

			public void addListener(IPackageManagerListener listener) throws RemoteException {
				if (mBinder.runsOnSameThread()) {
					mStub.addListener(IPackageManagerListener.Stub.asInterface(listener.asBinder()));
				} else {
					mProxy.addListener(listener);
				}
			}

			public void removeListener(IPackageManagerListener listener) throws RemoteException {
				if (mBinder.runsOnSameThread()) {
					mStub.removeListener(IPackageManagerListener.Stub.asInterface(listener.asBinder()));
				} else {
					mProxy.removeListener(listener);
				}
			}

			public ComponentName[] getAutostartServices() throws RemoteException {
				if (mBinder.runsOnSameThread()) {
					return mStub.getAutostartServices();
				} else {
					return mProxy.getAutostartServices();
				}
			}
		}
		
		static final int MSG_RESOLVE_SERVICE = 1;
		static final int MSG_ADD_LISTENER = 2;
		static final int MSG_REMOVE_LISTENER = 3;
		static final int MSG_GET_AUTOSTART_SERVICES = 4;
	}
	
	public ServiceInfo resolveService(Intent service) throws RemoteException;
	public void addListener(IPackageManagerListener listener) throws RemoteException;
	public void removeListener(IPackageManagerListener listener) throws RemoteException;
	public ComponentName[] getAutostartServices() throws RemoteException;
}
