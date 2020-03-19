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

import mindroid.os.Binder;
import mindroid.os.Bundle;
import mindroid.os.IBinder;
import mindroid.util.concurrent.Future;
import mindroid.util.concurrent.Promise;

import java.net.URI;

public class ServiceInfo {
    private final String mInterfaceDescriptor;
    private final Bundle mExtras;
    private final URI mUri;

    public ServiceInfo(String interfaceDescriptor, Bundle extras, URI uri) {
        mInterfaceDescriptor = interfaceDescriptor;
        mExtras = extras;
        mUri = uri;
    }

    public String getInterfaceDescriptor() {
        return mInterfaceDescriptor;
    }

    public Bundle getExtras() {
        if (mExtras == null){
            return new Bundle();
        } else {
            return new Bundle(mExtras);
        }
    }

    public URI getUri() {
        return mUri;
    }

    public Future<IBinder> connect() {
        return new Promise<>(new Binder.Proxy(getUri()));
    }
}
