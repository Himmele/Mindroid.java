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
				ComponentName caller = (ComponentName) data.getObject("caller");
				Intent service = (Intent) data.getObject("intent");
				ComponentName component;
				if (caller != null) {
					component = startService(caller, service);
				} else {
					component = startService(service);
				}
				return component;
			}
			case MSG_STOP_SERVICE: {
				ComponentName caller = (ComponentName) data.getObject("caller");
				Intent service = (Intent) data.getObject("intent");
				boolean result;
				if (caller != null) {
					result = stopService(caller, service);
				} else {
					result = stopService(service);
				}
				return new Boolean(result);
			}
			case MSG_BIND_SERVICE: {
				ComponentName caller = (ComponentName) data.getObject("caller");
				Intent service = (Intent) data.getObject("intent");
				ServiceConnection conn = (ServiceConnection) data.getObject("conn");
				int flags = data.getInt("flags");
				boolean result = bindService(caller, service, conn, flags);
				return new Boolean(result);
			}
			case MSG_UNBIND_SERVICE: {
				ComponentName caller = (ComponentName) data.getObject("caller");
				Intent service = (Intent) data.getObject("service");
				ServiceConnection conn = (ServiceConnection) data.getObject("conn");
				unbindService(caller, service, conn);
				return null;
			}
			case MSG_START_SYSTEM_SERVICE: {
				Intent service = (Intent) obj;
				ComponentName component = startSystemService(service);
				return component;
			}
			case MSG_STOP_SYSTEM_SERVICE: {
				Intent service = (Intent) obj;
				boolean result = stopSystemService(service);
				return new Boolean(result);
			}
			default:
			    return super.onTransact(what, arg1, arg2, obj, data);
			}
		}
		
		private static class Proxy implements IServiceManager {
			private final IBinder mBinder;
			
			Proxy(IBinder binder) {
				mBinder = binder;
			}
			
			public IBinder asBinder() {
				return mBinder;
			}
			
			public ComponentName startService(Intent service) throws RemoteException {
				return startService(null, service);
			}
			
			public boolean stopService(Intent service) throws RemoteException {
				return stopService(null, service);
			}
			
			public ComponentName startService(ComponentName caller, Intent service) throws RemoteException {
				Bundle data = new Bundle();
				data.putObject("caller", caller);
				data.putObject("intent", service);
				return (ComponentName) mBinder.transact(MSG_START_SERVICE, data, 0);
			}
			
			public boolean stopService(ComponentName caller, Intent service) throws RemoteException {
				Bundle data = new Bundle();
				data.putObject("caller", caller);
				data.putObject("intent", service);
				Boolean result = (Boolean) mBinder.transact(MSG_STOP_SERVICE, data, 0);
				return result.booleanValue();
			}
			
			public boolean bindService(ComponentName caller, Intent service, ServiceConnection conn, int flags) throws RemoteException {
				Bundle data = new Bundle();
				data.putObject("caller", caller);
				data.putObject("intent", service);
				data.putObject("conn", conn);
				data.putInt("flags", flags);
				Boolean result = (Boolean) mBinder.transact(MSG_BIND_SERVICE, data, 0);
				return result.booleanValue();
			}
			
			public void unbindService(ComponentName caller, Intent service, ServiceConnection conn) throws RemoteException {
				Bundle data = new Bundle();
				data.putObject("caller", caller);
				data.putObject("service", service);
				data.putObject("conn", conn);
				mBinder.transact(MSG_UNBIND_SERVICE, data, FLAG_ONEWAY);
			}

			public ComponentName startSystemService(Intent service) throws RemoteException {
				ComponentName component = (ComponentName) mBinder.transact(MSG_START_SYSTEM_SERVICE, service, 0);
				return component;
			}

			public boolean stopSystemService(Intent service) throws RemoteException {
				Boolean result = (Boolean) mBinder.transact(MSG_STOP_SYSTEM_SERVICE, service, 0);
				return result.booleanValue();
			}
		}
		
		private static class SmartProxy implements IServiceManager {
			private final IBinder mBinder;
			private final IServiceManager mStub;
			private final IServiceManager mProxy;
			
			SmartProxy(IBinder binder) {
				mBinder = binder;
				mStub = (IServiceManager) binder.queryLocalInterface(DESCRIPTOR);
				mProxy = new IServiceManager.Stub.Proxy(binder);
			}
			
			public IBinder asBinder() {
				return mBinder;
			}
			
			public ComponentName startService(Intent service) throws RemoteException {
				if (mBinder.runsOnSameThread()) {
					return mStub.startService(service);
				} else {
					return mProxy.startService(service);
				}
			}
			
			public boolean stopService(Intent service) throws RemoteException {
				if (mBinder.runsOnSameThread()) {
					return mStub.stopService(service);
				} else {
					return mProxy.stopService(service);
				}
			}
			
			public ComponentName startService(ComponentName caller, Intent service) throws RemoteException {
				if (mBinder.runsOnSameThread()) {
					return mStub.startService(caller, service);
				} else {
					return mProxy.startService(caller, service);
				}
			}
			
			public boolean stopService(ComponentName caller, Intent service) throws RemoteException {
				if (mBinder.runsOnSameThread()) {
					return mStub.stopService(caller, service);
				} else {
					return mProxy.stopService(caller, service);
				}
			}
			
			public boolean bindService(ComponentName caller, Intent service, ServiceConnection conn, int flags) throws RemoteException {
				if (mBinder.runsOnSameThread()) {
					return mStub.bindService(caller, service, conn, flags);
				} else {
					return mProxy.bindService(caller, service, conn, flags);
				}
			}
			
			public void unbindService(ComponentName caller, Intent service, ServiceConnection conn) throws RemoteException {
				if (mBinder.runsOnSameThread()) {
					mStub.unbindService(caller, service, conn);
				} else {
					mProxy.unbindService(caller, service, conn);
				}
			}

			public ComponentName startSystemService(Intent service) throws RemoteException {
				if (mBinder.runsOnSameThread()) {
					return mStub.startSystemService(service);
				} else {
					return mProxy.startSystemService(service);
				}
			}

			public boolean stopSystemService(Intent service) throws RemoteException {
				if (mBinder.runsOnSameThread()) {
					return mStub.stopSystemService(service);
				} else {
					return mProxy.stopSystemService(service);
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
	
	public ComponentName startService(Intent service) throws RemoteException;
	public boolean stopService(Intent service) throws RemoteException;
	public ComponentName startService(ComponentName caller, Intent service) throws RemoteException;
	public boolean stopService(ComponentName caller, Intent service) throws RemoteException;
	public boolean bindService(ComponentName caller, Intent service, ServiceConnection conn, int flags) throws RemoteException;
	public void unbindService(ComponentName caller, Intent service, ServiceConnection conn) throws RemoteException;
	public ComponentName startSystemService(Intent service) throws RemoteException;
	public boolean stopSystemService(Intent service) throws RemoteException;
}
