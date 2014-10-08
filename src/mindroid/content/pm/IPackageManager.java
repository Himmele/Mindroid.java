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
import mindroid.os.Message;
import mindroid.util.concurrent.SettableFuture;

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
			return new IPackageManager.Stub.SmartProxy((Binder) binder);
		}
		
		public IBinder asBinder() {
			return this;
		}
		
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_RESOLVE_SERVICE: {
				Intent service = (Intent) msg.getData().getObject("intent");
				SettableFuture future = (SettableFuture) msg.getData().getObject("future");
				ServiceInfo serviceInfo = resolveService(service);
				future.set(serviceInfo);
				break;
			}
			case MSG_ADD_LISTENER: {
				addListener(IPackageManagerListener.Stub.asInterface((IBinder) msg.obj));
				break;
			}
			case MSG_REMOVE_LISTENER: {
				removeListener(IPackageManagerListener.Stub.asInterface((IBinder) msg.obj));
				break;
			}
			case MSG_GET_AUTOSTART_SERVICES: {
				SettableFuture future = (SettableFuture) msg.obj;
				ComponentName[] components = getAutostartServices();
				future.set(components);
				break;
			}
			default:
			    super.handleMessage(msg);
			}
		}
		
		private static class Proxy implements IPackageManager {
			private final Binder mBinder;
			
			Proxy(Binder binder) {
				mBinder = binder;
			}
			
			public IBinder asBinder() {
				return mBinder;
			}
			
			public ServiceInfo resolveService(Intent service) {
				SettableFuture future = new SettableFuture();
				Message msg = mBinder.obtainMessage(MSG_RESOLVE_SERVICE);
				Bundle data = new Bundle();
				data.putObject("intent", service);
				data.putObject("future", future);
				msg.setData(data);
				msg.sendToTarget();
				ServiceInfo serviceInfo = null;
				serviceInfo = (ServiceInfo) future.get();
				return serviceInfo;
			}

			public void addListener(IPackageManagerListener listener) {
				Message msg = mBinder.obtainMessage(MSG_ADD_LISTENER, listener.asBinder());
				msg.sendToTarget();
			}

			public void removeListener(IPackageManagerListener listener) {
				Message msg = mBinder.obtainMessage(MSG_REMOVE_LISTENER, listener.asBinder());
	        	msg.sendToTarget();
			}

			public ComponentName[] getAutostartServices() {
				SettableFuture future = new SettableFuture();
				Message msg = mBinder.obtainMessage(MSG_GET_AUTOSTART_SERVICES, future);
	        	msg.sendToTarget();
	        	ComponentName[] components = null;
				components = (ComponentName[]) future.get();
				return components;
			}
		}
		
		private static class SmartProxy implements IPackageManager {
			private final Binder mBinder;
			private final IPackageManager mServiceReference;
			private final Proxy mProxy;
			
			SmartProxy(Binder binder) {
				mBinder = binder;
				mServiceReference = (IPackageManager) binder.queryInterface(DESCRIPTOR);
				mProxy = new IPackageManager.Stub.Proxy(binder);
			}
			
			public IBinder asBinder() {
				return mBinder;
			}
			
			public ServiceInfo resolveService(Intent service) {
				if (mBinder.checkThread()) {
					return mServiceReference.resolveService(service);
				} else {
					return mProxy.resolveService(service);
				}
			}

			public void addListener(IPackageManagerListener listener) {
				if (mBinder.checkThread()) {
					mServiceReference.addListener(IPackageManagerListener.Stub.asInterface(listener.asBinder()));
				} else {
					mProxy.addListener(listener);
				}
			}

			public void removeListener(IPackageManagerListener listener) {
				if (mBinder.checkThread()) {
					mServiceReference.removeListener(IPackageManagerListener.Stub.asInterface(listener.asBinder()));
				} else {
					mProxy.removeListener(listener);
				}
			}

			public ComponentName[] getAutostartServices() {
				if (mBinder.checkThread()) {
					return mServiceReference.getAutostartServices();
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
	
	public abstract ServiceInfo resolveService(Intent service);
	public abstract void addListener(IPackageManagerListener listener);
	public abstract void removeListener(IPackageManagerListener listener);
	public abstract ComponentName[] getAutostartServices();
}
