/*
 * Copyright (C) 2006 The Android Open Source Project
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
import mindroid.content.Context;
import mindroid.content.Intent;
import mindroid.os.RemoteException;
import mindroid.os.ServiceManager;

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

    /**
     * Permission check result: this is returned by {@link #checkPermission}
     * if the permission has been granted to the given package.
     */
    public static final int PERMISSION_GRANTED = 0;

    /**
     * Permission check result: this is returned by {@link #checkPermission}
     * if the permission has not been granted to the given package.
     */
    public static final int PERMISSION_DENIED = -1;

    /**
     * @hide
     */
    public PackageManager() {
        mService = IPackageManager.Stub.asInterface(ServiceManager.getSystemService(Context.PACKAGE_MANAGER));
    }

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
    public List<PackageInfo> getInstalledPackages(int flags) {
        try {
            return mService.getInstalledPackages(flags);
        } catch (RemoteException e) {
            throw new RuntimeException("System failure", e);
        }
    }

    /**
     * Retrieve overall information about an application package that is
     * installed on the system.
     * <p>
     * Throws {@link NameNotFoundException} if a package with the given name can
     * not be found on the system.
     *
     * @param packageName The full name (i.e. com.google.apps.contacts) of the
     *            desired package.
     * @param flags Additional option flags. Use any combination of
     *            {@link #GET_SERVICES}, to modify the data returned.
     * @return Returns a PackageInfo object containing information about the
     *         package.
     *
     * @see #GET_SERVICES
     */
    public PackageInfo getPackageInfo(String packageName, int flags) {
        try {
            return mService.getPackageInfo(packageName, flags);
        } catch (RemoteException e) {
            throw new RuntimeException("System failure", e);
        }
    }

    /**
     * Retrieve overall information about an application package defined
     * in a package archive file
     *
     * @param archiveFilePath The path to the archive file
     * @param flags Additional option flags. Use any combination of
     * {@link #GET_SERVICES}, to modify the data returned.
     *
     * @return Returns the information about the package. Returns
     * null if the package could not be successfully parsed.
     *
     * @see #GET_SERVICES
     *
     */
    public PackageInfo getPackageArchiveInfo(String archiveFilePath, int flags) {
        try {
            return mService.getPackageArchiveInfo(archiveFilePath, flags);
        } catch (RemoteException e) {
            throw new RuntimeException("System failure", e);
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
            throw new RuntimeException("System failure", e);
        }
    }

    /**
     * Check whether a particular package has been granted a particular
     * permission.
     *
     * @param permissionName The name of the permission you are checking for.
     * @param pid The process id you are checking against.
     *
     * @return If the package has the permission, PERMISSION_GRANTED is
     * returned.  If it does not have the permission, PERMISSION_DENIED
     * is returned.
     *
     * @see #PERMISSION_GRANTED
     * @see #PERMISSION_DENIED
     */
    public int checkPermission(String permissionName, int pid) {
        try {
            return mService.checkPermission(permissionName, pid);
        } catch (RemoteException e) {
            throw new RuntimeException("System failure", e);
        }
    }

    public String[] getPermissions(int pid) {
        try {
            return mService.getPermissions(pid);
        } catch (RemoteException e) {
            throw new RuntimeException("System failure", e);
        }
    }

    public void addListener(PackageManagerListener listener) throws RemoteException {
        try {
            mService.addListener(listener.asInterface());
        } catch (RemoteException e) {
            throw new RuntimeException("System failure", e);
        }
    }

    public void removeListener(PackageManagerListener listener) throws RemoteException {
        try {
            mService.removeListener(listener.asInterface());
        } catch (RemoteException e) {
            throw new RuntimeException("System failure", e);
        }
    }
}
