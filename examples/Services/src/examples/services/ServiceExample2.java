/*
 * Copyright (C) 2018 Daniel Himmelein
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

package examples.services;

import mindroid.app.Service;
import mindroid.content.Intent;
import mindroid.os.Binder;
import mindroid.os.IBinder;
import mindroid.util.Log;

public class ServiceExample2 extends Service {
    private static final String LOG_TAG = "ServiceExample2";

    public void onCreate() {
        Log.i(LOG_TAG, "onCreate");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOG_TAG, "onStartCommand: " + startId);
        return 0;
    }

    public IBinder onBind(Intent intent) {
        Binder binder = new Binder();
        Log.i(LOG_TAG, "onBind: " + binder);
        return binder;
    }

    public boolean onUnbind(Intent intent) {
        Log.i(LOG_TAG, "onUnbind");
        return true;
    }

    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy");
    }
}
