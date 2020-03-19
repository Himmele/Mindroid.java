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

import mindroid.os.Bundle;
import mindroid.runtime.system.Runtime;

import java.net.URISyntaxException;

public class ServiceDiscoveryManager {
    /**
     * Initiate service discovery to browse for instances of a service.
     *
     * @param interfaceDescriptor the interface descriptor of the service to find.
     * @param extras the extra parameters.
     * @param listener the listener that notifies about discovery events.
     */
    public void discoverServices(String interfaceDescriptor, Bundle extras, DiscoveryListener listener) throws URISyntaxException {
        Runtime.getRuntime().discoverServices(interfaceDescriptor, extras, listener);
    }

    /**
     * Stop service discovery initiated with {@link #discoverServices}.
     *
     * @param listener the listener that was passed to {@link #discoverServices(String, Bundle, DiscoveryListener)}.
     */
    public void stopServiceDiscovery(DiscoveryListener listener) {
        Runtime.getRuntime().stopServiceDiscovery(listener);
    }
}
