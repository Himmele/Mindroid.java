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

import static mindroid.util.concurrent.AsyncAwait.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import mindroid.app.Service;
import mindroid.content.ComponentName;
import mindroid.content.Intent;
import mindroid.os.IBinder;
import mindroid.os.IServiceManager;
import mindroid.os.RemoteException;
import mindroid.os.ServiceManager;
import mindroid.testing.IntegrationTest;
import mindroid.util.Log;
import mindroid.util.concurrent.ExecutionException;
import mindroid.util.concurrent.Promise;
import mindroid.util.logging.Logger;

public class Services extends IntegrationTest {
    @Test
    void test1() {
        Logger logger = new Logger();

        Promise<?> p = async()
        .thenCompose(value -> {
            return startService();
        })
        .thenCompose(value -> {
            return logger.assumeThat("ServiceManager", "Service Services\\$TestService has been created in process mindroid.tests", 10000);
        })
        .thenCompose(value -> {
            return logger.assumeThat("TestService", "onStartCommand", 10000);
        })
        .thenCompose(value -> {
            return stopService();
        })
        .thenCompose(value -> {
            return logger.assumeThat("ServiceManager", "Service Services\\$TestService has been stopped", 10000);
        });

        try {
            await(p, 10000);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    void test2() {
        try {
            Logger logger = new Logger();

            startService().get(10000);
            logger.assumeThat("ServiceManager", "Service Services\\$TestService has been created in process mindroid.tests", 10000).get(10000);
            logger.assumeThat("TestService", "onStartCommand", 10000).get(10000);
            stopService().get(10000);
            logger.assumeThat("ServiceManager", "Service Services\\$TestService has been stopped", 10000).get(10000);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private Promise<ComponentName> startService() {
        IServiceManager serviceManager = ServiceManager.getServiceManager();
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("mindroid.tests", "Services$TestService"));
        try {
            return serviceManager.startSystemService(intent);
        } catch (RemoteException e) {
            fail(e.getMessage());
            return new Promise<>(new ExecutionException());
        }
    }

    private Promise<Boolean> stopService() {
        IServiceManager serviceManager = ServiceManager.getServiceManager();
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("mindroid.tests", "Services$TestService"));
        try {
            return serviceManager.stopSystemService(intent);
        } catch (RemoteException e) {
            fail(e.getMessage());
            return new Promise<>(new ExecutionException());
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
