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

import mindroid.os.Bundle;
import mindroid.os.IInterface;
import mindroid.os.IBinder;
import mindroid.os.Binder;
import mindroid.os.RemoteException;

public interface IPackageManagerListener extends IInterface {
	public static abstract class Stub extends Binder implements IPackageManagerListener {
		private static final java.lang.String DESCRIPTOR = "mindroid.content.pm.IPackageManagerListener";
		
		public Stub() {
			this.attachInterface(this, DESCRIPTOR);
		}
		
		public static mindroid.content.pm.IPackageManagerListener asInterface(IBinder binder) {
			if (binder == null) {
				return null;
			}
			return new IPackageManagerListener.Stub.SmartProxy(binder);
		}
		
		public IBinder asBinder() {
			return this;
		}
		
		protected Object onTransact(int what, int arg1, int arg2, Object obj, Bundle data) throws RemoteException {
			switch (what) {
			case MSG_NOTIFY_BOOT_COMPLETED: {
				onBootCompleted();
				return null;
			}
			default:
			    return super.onTransact(what, arg1, arg2, obj, data);
			}
		}
		
		private static class Proxy implements IPackageManagerListener {
			private final IBinder mBinder;

			Proxy(IBinder binder) {
				mBinder = binder;
			}
			
			public IBinder asBinder() {
				return mBinder;
			}

			public void onBootCompleted() throws RemoteException {
				mBinder.transact(MSG_NOTIFY_BOOT_COMPLETED, FLAG_ONEWAY);
			}
		}
		
		private static class SmartProxy implements IPackageManagerListener {
			private final IBinder mBinder;
			private final IPackageManagerListener mStub;
			private final IPackageManagerListener mProxy;

			SmartProxy(IBinder binder) {
				mBinder = binder;
				mStub = (mindroid.content.pm.IPackageManagerListener) binder.queryLocalInterface(DESCRIPTOR);
				mProxy = new mindroid.content.pm.IPackageManagerListener.Stub.Proxy(binder);
			}
			
			public IBinder asBinder() {
				return mBinder;
			}

			public void onBootCompleted() throws RemoteException {
				if (mBinder.runsOnSameThread()) {
					mStub.onBootCompleted();
				} else {
					mProxy.onBootCompleted();
				}
			}
		}
		
		static final int MSG_NOTIFY_BOOT_COMPLETED = 1;
	}
	
	public void onBootCompleted() throws RemoteException;
}
