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
import mindroid.content.ComponentName;
import mindroid.content.Intent;
import mindroid.content.ServiceConnection;
import mindroid.os.Handler;
import mindroid.os.IBinder;
import mindroid.util.Log;

public class ServiceExample1 extends Service {
    private static final String LOG_TAG = "ServiceExample1";
    private final Handler mHandler = new Handler();

    public void onCreate() {
        final Intent intent = new Intent();
        intent.setClassName("examples.services", "ServiceExample2");
        final ServiceConnection conn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.i(LOG_TAG, "onServiceConnected: " + service);
            }

            public void onServiceDisconnected(ComponentName name) {
                Log.i(LOG_TAG, "onServiceDisconnected");
            }

        };
        bindService(intent, conn, 0);
        startService(intent);

        mHandler.postDelayed(new Runnable() {
            public void run() {
                stopService(intent);
            }
        }, 8000);

        mHandler.postDelayed(new Runnable() {
            public void run() {
                unbindService(conn);
            }
        }, 4000);

        mHandler.postDelayed(new Runnable() {
            public void run() {
                Log.i(LOG_TAG, "Test");
                mHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    public IBinder onBind(Intent intent) {
        return null;
    }
}
