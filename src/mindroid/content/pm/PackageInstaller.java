/*
 * Copyright (C) 2017 Daniel Himmelein
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

import java.io.File;
import mindroid.content.Context;
import mindroid.os.RemoteException;
import mindroid.os.ServiceManager;

public class PackageInstaller {
    private IPackageInstaller mService;

    public PackageInstaller() {
        mService = IPackageInstaller.Stub.asInterface(ServiceManager.getSystemService(Context.PACKAGE_INSTALLER));
    }

    public PackageInstaller(Context context) {
        mService = IPackageInstaller.Stub.asInterface(context.getSystemService(Context.PACKAGE_INSTALLER));
    }

    public void install(File app) {
        try {
            mService.install(app);
        } catch (RemoteException e) {
            throw new RuntimeException("System failure", e);
        }
    }

    public void install(String packageName, String[] classNames) {
        try {
            mService.install(packageName, classNames);
        } catch (RemoteException e) {
            throw new RuntimeException("System failure", e);
        }
    }

    public void uninstall(String packageName) {
        try {
            mService.uninstall(packageName);
        } catch (RemoteException e) {
            throw new RuntimeException("System failure", e);
        }
    }
}
