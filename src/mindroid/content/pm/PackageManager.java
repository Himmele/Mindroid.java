/*
 * Copyright (C) 2006 The Android Open Source Project
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

package mindroid.content.pm;

import java.util.List;
import mindroid.content.Context;
import mindroid.content.Intent;
import mindroid.os.RemoteException;

/**
 * Class for retrieving various kinds of information related to the application packages that are
 * currently installed on the device.
 * 
 * You can find this class through {@link Context#getPackageManager}.
 */
public class PackageManager {
	private IPackageManager mService;

	/**
	 * {@link PackageInfo} flag: return information about services in the package in
	 * {@link PackageInfo#services}.
	 */
	public static final int GET_SERVICES = 0x00000004;

	public PackageManager(Context context) {
		mService = IPackageManager.Stub.asInterface(context.getSystemService(Context.PACKAGE_MANAGER));
	}

	/**
	 * Return a List of all packages that are installed on the device.
	 * 
	 * @param flags Additional option flags. Use any combination of {@link #GET_SERVICES}
	 * 
	 * @return A List of PackageInfo objects, one for each package that is installed on the device.
	 * In the unlikely case of there being no installed packages, an empty list is returned.
	 * 
	 * @see #GET_SERVICES
	 */
	public List getInstalledPackages(int flags) {
		try {
			return mService.getInstalledPackages(flags);
		} catch (RemoteException e) {
			throw new RuntimeException("System failure");
		}
	}

	/**
	 * Determine the best service to handle for a given Intent.
	 * 
	 * @param intent An intent containing all of the desired specification (action, data, type,
	 * category, and/or component).
	 * @param flags Additional option flags.
	 * 
	 * @return Returns a ResolveInfo containing the final service intent that was determined to be
	 * the best action. Returns null if no matching service was found.
	 */
	public ResolveInfo resolveService(Intent intent, int flags) {
		try {
			return mService.resolveService(intent, flags);
		} catch (RemoteException e) {
			throw new RuntimeException("System failure");
		}
	}

	public void addListener(PackageManagerListener listener) throws RemoteException {
		try {
			mService.addListener(listener.asInterface());
		} catch (RemoteException e) {
			throw new RuntimeException("System failure");
		}
	}

	public void removeListener(PackageManagerListener listener) throws RemoteException {
		try {
			mService.removeListener(listener.asInterface());
		} catch (RemoteException e) {
			throw new RuntimeException("System failure");
		}
	}
}
