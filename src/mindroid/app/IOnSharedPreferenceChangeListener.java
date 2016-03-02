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

package mindroid.app;

import mindroid.content.SharedPreferences;
import mindroid.os.Bundle;
import mindroid.os.IInterface;
import mindroid.os.IBinder;
import mindroid.os.Binder;
import mindroid.os.RemoteException;

public interface IOnSharedPreferenceChangeListener extends IInterface {
	public static abstract class Stub extends Binder implements IOnSharedPreferenceChangeListener {
		private static final java.lang.String DESCRIPTOR = "mindroid.app.IOnSharedPreferenceChangeListener";

		public Stub() {
			this.attachInterface(this, DESCRIPTOR);
		}

		public static mindroid.app.IOnSharedPreferenceChangeListener asInterface(IBinder binder) {
			if (binder == null) {
				return null;
			}
			return new IOnSharedPreferenceChangeListener.Stub.SmartProxy(binder);
		}

		public IBinder asBinder() {
			return this;
		}

		protected Object onTransact(int what, int arg1, int arg2, Object obj, Bundle data) throws RemoteException {
			switch (what) {
			case MSG_ON_SHARED_PREFERENCE_CHANGED: {
				SharedPreferences sharedPreferences = (SharedPreferences) data.getObject("sharedPreferences");
				String key = data.getString("key");
				onSharedPreferenceChanged(sharedPreferences, key);
				return null;
			}
			default:
				return super.onTransact(what, arg1, arg2, obj, data);
			}
		}

		private static class Proxy implements IOnSharedPreferenceChangeListener {
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

			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) throws RemoteException {
				Bundle data = new Bundle();
				data.putObject("sharedPreferences", sharedPreferences);
				data.putString("key", key);
				mRemote.transact(MSG_ON_SHARED_PREFERENCE_CHANGED, data, FLAG_ONEWAY);
			}
		}

		private static class SmartProxy implements IOnSharedPreferenceChangeListener {
			private final IBinder mRemote;
			private final IOnSharedPreferenceChangeListener mStub;
			private final IOnSharedPreferenceChangeListener mProxy;

			SmartProxy(IBinder remote) {
				mRemote = remote;
				mStub = (mindroid.app.IOnSharedPreferenceChangeListener) remote.queryLocalInterface(DESCRIPTOR);
				mProxy = new mindroid.app.IOnSharedPreferenceChangeListener.Stub.Proxy(remote);
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

			public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) throws RemoteException {
				if (mRemote.runsOnSameThread()) {
					mStub.onSharedPreferenceChanged(sharedPreferences, key);
				} else {
					mProxy.onSharedPreferenceChanged(sharedPreferences, key);
				}
			}
		}

		static final int MSG_ON_SHARED_PREFERENCE_CHANGED = 1;
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) throws RemoteException;
}
