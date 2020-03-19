/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2020 E.S.R.Labs
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

package mindroid.runtime.sd;

import mindroid.os.RemoteException;
import mindroid.runtime.system.Plugin;

public abstract class DiscoveryListener {
    private Plugin mPlugin;

    public void onDiscoveryStarted() {
    }

    public void onDiscoveryStopped() {
    }

    public abstract void onServiceFound(ServiceInfo service);
    public abstract void onServiceLost(ServiceInfo service);

    public abstract void onStartDiscoveryFailed(Throwable cause);
    public abstract void onStopDiscoveryFailed(Throwable cause);

    /**
     * @hide
     */
    public Plugin getPlugin() {
        return mPlugin;
    }

    /**
     * @hide
     */
    public void setPlugin(Plugin plugin) {
        mPlugin = plugin;
    }

    /**
     * @hide
     */
    public IDiscoveryListener asInterface() {
        return mWrapper;
    }

    private final IDiscoveryListener.Stub mWrapper = new IDiscoveryListener.Stub() {
        @Override
        public void onDiscoveryStarted() throws RemoteException {
            DiscoveryListener.this.onDiscoveryStarted();
        }

        @Override
        public void onDiscoveryStopped() throws RemoteException {
            DiscoveryListener.this.onDiscoveryStopped();
        }

        @Override
        public void onServiceFound(ServiceInfo service) throws RemoteException {
            DiscoveryListener.this.onServiceFound(service);
        }

        @Override
        public void onServiceLost(ServiceInfo service) throws RemoteException {
            DiscoveryListener.this.onServiceLost(service);
        }

        @Override
        public void onStartDiscoveryFailed(Throwable cause) throws RemoteException {
            DiscoveryListener.this.onStartDiscoveryFailed(cause);
        }

        @Override
        public void onStopDiscoveryFailed(Throwable cause) throws RemoteException {
            DiscoveryListener.this.onStopDiscoveryFailed(cause);
        }
    };
}
