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

package mindroid.tests;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import mindroid.app.Service;
import mindroid.content.ComponentName;
import mindroid.content.Intent;
import mindroid.os.Bundle;
import mindroid.os.IBinder;
import mindroid.os.IServiceManager;
import mindroid.os.RemoteException;
import mindroid.os.ServiceManager;
import mindroid.testing.IntegrationTest;
import mindroid.util.Log;
import mindroid.util.concurrent.CancellationException;
import mindroid.util.concurrent.ExecutionException;
import mindroid.util.concurrent.Executors;
import mindroid.util.concurrent.Promise;
import mindroid.util.concurrent.TimeoutException;

public class Services extends IntegrationTest {
    @Test
    void test() {
        Promise<Boolean> promise = new Promise<>(Executors.SYNCHRONOUS_EXECUTOR);

        IServiceManager serviceManager = ServiceManager.getServiceManager();
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("mindroid.tests", "Services$TestService"));
        Bundle test = new Bundle();
        test.putObject("promise", promise);
        intent.putExtra("test", test);
        try {
            serviceManager.startSystemService(intent).get(10000);
        } catch (CancellationException | ExecutionException | TimeoutException | InterruptedException | RemoteException e) {
            fail(e.getMessage());
        }

        // FIXME: Wait for onStartCommand log.
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        try {
            serviceManager.stopSystemService(intent).get(10000);
        } catch (CancellationException | ExecutionException | TimeoutException | InterruptedException | RemoteException e) {
            fail(e.getMessage());
        }

        try {
            Promise.allOf(Executors.SYNCHRONOUS_EXECUTOR, promise).orTimeout(10000).get();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public static class TestService extends Service {
        private static final String TAG = "TestService";

        @Override
        public void onCreate() {
            Log.d(TAG, "onCreate");
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.d(TAG, "onStartCommand");

            Bundle test = intent.getBundleExtra("test");
            if (test != null && test.containsKey("promise")) {
                Promise<Boolean> promise = (Promise<Boolean>) test.getObject("promise");
                promise.complete(true);
            }
            return 0;
        }

        @Override
        public void onDestroy() {
            Log.d(TAG, "onDestroy");
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }
}
