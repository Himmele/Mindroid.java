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
import mindroid.os.Message;
import mindroid.util.concurrent.SettableFuture;

public interface IServiceManager extends IInterface {
	public static abstract class Stub extends Binder implements IServiceManager {
		private static final java.lang.String DESCRIPTOR = "mindroid.os.IServiceManager";
		
		public Stub(Looper looper) {
			super(looper);
			this.attachInterface(this, DESCRIPTOR, looper.getThread());
		}
		
		public static IServiceManager asInterface(IBinder binder) {
			if(binder == null) {
				return null;
			}
			return new IServiceManager.Stub.SmartProxy((Binder) binder);
		}
		
		public IBinder asBinder() {
			return this;
		}
		
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_START_SERVICE: {
				ComponentName caller = (ComponentName) msg.getData().getObject("caller");
				Intent service = (Intent) msg.getData().getObject("intent");
				SettableFuture future = (SettableFuture) msg.getData().getObject("future");
				ComponentName component;
				if (caller != null) {
					component = startService(caller, service);
				} else {
					component = startService(service);
				}
				future.set(component);
				break;
			}
			case MSG_STOP_SERVICE: {
				ComponentName caller = (ComponentName) msg.getData().getObject("caller");
				Intent service = (Intent) msg.getData().getObject("intent");
				SettableFuture future = (SettableFuture) msg.getData().getObject("future");
				boolean result;
				if (caller != null) {
					result = stopService(caller, service);
				} else {
					result = stopService(service);
				}
				future.set(new Boolean(result));
				break;
			}
			case MSG_BIND_SERVICE: {
				ComponentName caller = (ComponentName) msg.getData().getObject("caller");
				Intent service = (Intent) msg.getData().getObject("intent");
				ServiceConnection conn = (ServiceConnection) msg.getData().getObject("conn");
				int flags = msg.getData().getInt("flags");
				SettableFuture future = (SettableFuture) msg.getData().getObject("future");
				boolean result = bindService(caller, service, conn, flags);
				future.set(new Boolean(result));
				break;
			}
			case MSG_UNBIND_SERVICE: {
				ComponentName caller = (ComponentName) msg.getData().getObject("caller");
				Intent service = (Intent) msg.getData().getObject("service");
				ServiceConnection conn = (ServiceConnection) msg.getData().getObject("conn");
				unbindService(caller, service, conn);
				break;
			}
			case MSG_START_SYSTEM_SERVICE: {
				Intent service = (Intent) msg.getData().getObject("intent");
				SettableFuture future = (SettableFuture) msg.getData().getObject("future");
				ComponentName component = startSystemService(service);
				future.set(component);
				break;
			}
			case MSG_STOP_SYSTEM_SERVICE: {
				Intent service = (Intent) msg.getData().getObject("intent");
				SettableFuture future = (SettableFuture) msg.getData().getObject("future");
				boolean result = stopSystemService(service);
				future.set(new Boolean(result));
				break;
			}
			default:
			    super.handleMessage(msg);
			}
		}
		
		private static class Proxy implements IServiceManager {
			private final Binder mBinder;
			
			Proxy(Binder binder) {
				mBinder = binder;
			}
			
			public IBinder asBinder() {
				return mBinder;
			}
			
			public ComponentName startService(Intent service) {
				return startService(null, service);
			}
			
			public boolean stopService(Intent service) {
				return stopService(null, service);
			}
			
			public ComponentName startService(ComponentName caller, Intent service) {
				SettableFuture future = new SettableFuture();
				Message msg = mBinder.obtainMessage(MSG_START_SERVICE);
				Bundle data = new Bundle();
				data.putObject("caller", caller);
				data.putObject("intent", service);
				data.putObject("future", future);
				msg.setData(data);
				msg.sendToTarget();
				ComponentName component = (ComponentName) future.get();
				return component;
			}
			
			public boolean stopService(ComponentName caller, Intent service) {
				SettableFuture future = new SettableFuture();
				Message msg = mBinder.obtainMessage(MSG_STOP_SERVICE);
				Bundle data = new Bundle();
				data.putObject("caller", caller);
				data.putObject("intent", service);
				data.putObject("future", future);
				msg.setData(data);
				msg.sendToTarget();
				Boolean result = (Boolean) future.get();
				return result.booleanValue();
			}
			
			public boolean bindService(ComponentName caller, Intent service, ServiceConnection conn, int flags) {
				SettableFuture future = new SettableFuture();
				Message msg = mBinder.obtainMessage(MSG_BIND_SERVICE);
				Bundle data = new Bundle();
				data.putObject("caller", caller);
				data.putObject("intent", service);
				data.putObject("conn", conn);
				data.putInt("flags", flags);
				data.putObject("future", future);
				msg.setData(data);
				msg.sendToTarget();
				Boolean result = (Boolean) future.get();
				return result.booleanValue();
			}
			
			public void unbindService(ComponentName caller, Intent service, ServiceConnection conn) {
				Message msg = mBinder.obtainMessage(MSG_UNBIND_SERVICE);
				Bundle data = new Bundle();
				data.putObject("caller", caller);
				data.putObject("service", service);
				data.putObject("conn", conn);
				msg.setData(data);
				msg.sendToTarget();
			}

			public ComponentName startSystemService(Intent service) {
				SettableFuture future = new SettableFuture();
				Message msg = mBinder.obtainMessage(MSG_START_SYSTEM_SERVICE);
				Bundle data = new Bundle();
				data.putObject("intent", service);
				data.putObject("future", future);
				msg.setData(data);
				msg.sendToTarget();
				ComponentName component = (ComponentName) future.get();
				return component;
			}

			public boolean stopSystemService(Intent service) {
				SettableFuture future = new SettableFuture();
				Message msg = mBinder.obtainMessage(MSG_STOP_SYSTEM_SERVICE);
				Bundle data = new Bundle();
				data.putObject("intent", service);
				data.putObject("future", future);
				msg.setData(data);
				msg.sendToTarget();
				Boolean result = (Boolean) future.get();
				return result.booleanValue();
			}
		}
		
		private static class SmartProxy implements IServiceManager {
			private final Binder mBinder;
			private final IServiceManager mServiceReference;
			private final Proxy mProxy;
			
			SmartProxy(Binder binder) {
				mBinder = binder;
				mServiceReference = (IServiceManager) binder.queryInterface(DESCRIPTOR);
				mProxy = new IServiceManager.Stub.Proxy(binder);
			}
			
			public IBinder asBinder() {
				return mBinder;
			}

			
			public ComponentName startService(Intent service) {
				if (mBinder.sameThread()) {
					return mServiceReference.startService(service);
				} else {
					return mProxy.startService(service);
				}
			}
			
			public boolean stopService(Intent service) {
				if (mBinder.sameThread()) {
					return mServiceReference.stopService(service);
				} else {
					return mProxy.stopService(service);
				}
			}
			
			public ComponentName startService(ComponentName caller, Intent service) {
				if (mBinder.sameThread()) {
					return mServiceReference.startService(caller, service);
				} else {
					return mProxy.startService(caller, service);
				}
			}
			
			public boolean stopService(ComponentName caller, Intent service) {
				if (mBinder.sameThread()) {
					return mServiceReference.stopService(caller, service);
				} else {
					return mProxy.stopService(caller, service);
				}
			}
			
			public boolean bindService(ComponentName caller, Intent service, ServiceConnection conn, int flags) {
				if (mBinder.sameThread()) {
					return mServiceReference.bindService(caller, service, conn, flags);
				} else {
					return mProxy.bindService(caller, service, conn, flags);
				}
			}
			
			public void unbindService(ComponentName caller, Intent service, ServiceConnection conn) {
				if (mBinder.sameThread()) {
					mServiceReference.unbindService(caller, service, conn);
				} else {
					mProxy.unbindService(caller, service, conn);
				}
			}

			public ComponentName startSystemService(Intent service) {
				if (mBinder.sameThread()) {
					return mServiceReference.startSystemService(service);
				} else {
					return mProxy.startSystemService(service);
				}
			}

			public boolean stopSystemService(Intent service) {
				if (mBinder.sameThread()) {
					return mServiceReference.stopSystemService(service);
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
	
	public abstract ComponentName startService(Intent service);
	public abstract boolean stopService(Intent service);
	public abstract ComponentName startService(ComponentName caller, Intent service);
	public abstract boolean stopService(ComponentName caller, Intent service);
	public abstract boolean bindService(ComponentName caller, Intent service, ServiceConnection conn, int flags);
	public abstract void unbindService(ComponentName caller, Intent service, ServiceConnection conn);
	public abstract ComponentName startSystemService(Intent service);
	public abstract boolean stopSystemService(Intent service);
}
