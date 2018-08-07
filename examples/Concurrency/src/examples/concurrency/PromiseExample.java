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

package examples.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import mindroid.app.Service;
import mindroid.content.Intent;
import mindroid.os.Handler;
import mindroid.os.IBinder;
import mindroid.os.SystemClock;
import mindroid.util.Log;
import mindroid.util.concurrent.Promise;

public class PromiseExample extends Service {
    private static final String LOG_TAG = "PromiseExample";
    ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    Promise<Integer> mPromise1 = new Promise<>();
    Promise<Integer> mPromise2 = new Promise<>();
    Promise<Integer> mPromise3 = new Promise<>();
    Promise<Integer> mPromise4 = new Promise<>();
    private Handler mHandler = new Handler();

    public void onCreate() {
        Log.i(LOG_TAG, "onCreate");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        mPromise1.completeWith(mPromise2);
        mPromise1.orTimeout(10000)
        .then((value) -> {
            Log.i(LOG_TAG, "Promise stage 1: " + value);
            if (System.currentTimeMillis() % 2 == 0) {
                throw new RuntimeException("Test");
            }
        }).then((value) -> {
            Log.i(LOG_TAG, "Promise stage 2: " + value);
            return 123;
        }).catchException(exception -> {
            Log.i(LOG_TAG, "Promise error stage 1: " + exception);
        }).then(mExecutorService, (value, exception) -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            }
            if (exception != null) {
                Log.i(LOG_TAG, "Promise error stage 2: " + exception);
            } else {
                Log.i(LOG_TAG, "Promise stage 3: " + value);
            }
        }).then((value, exception) -> {
            if (exception != null) {
                Log.i(LOG_TAG, "Promise error stage 3: " + exception);
            } else {
                Log.i(LOG_TAG, "Promise stage 4: " + value);
            }
            return 12345;
        }).then((value) -> {
            Log.i(LOG_TAG, "Promise stage 5: " + value);
        }).then(() -> {
            Log.i(LOG_TAG, "Promise stage 6");
        });

        new Handler().postDelayed(() -> {
            mPromise2.complete(42);
        }, 5000);


        action1(42)
            .thenCompose((value, exception) -> action2(value, exception))
            .thenCompose(mExecutorService, value -> action3(value))
            .thenCompose(value -> action4(value))
            .then(value -> { Log.i(LOG_TAG, "Result: " + value); });


        new Promise<>(42)
            .thenApply(value -> String.valueOf(value))
            .thenAccept(value -> Log.i(LOG_TAG, "Result: " + value));


        new Handler().postDelayed(() -> {
            mPromise3.complete(17);
        }, 10000);

        new Handler().postDelayed(() -> {
            mPromise4.complete(0);
        }, 7500);

        long startTime = SystemClock.uptimeMillis();
        Promise<Void> allOf = Promise.allOf(mPromise1, mPromise2, mPromise3, mPromise4);
        allOf.then((value, exception) -> {
            long now = SystemClock.uptimeMillis();
            Log.i(LOG_TAG, "AllOf result after " + (now - startTime) + "ms: " + value);
        });

        Promise<Object> anyOf = Promise.anyOf(mPromise1, mPromise2, mPromise3, mPromise4);
        anyOf.then((value, exception) -> {
            long now = SystemClock.uptimeMillis();
            Log.i(LOG_TAG, "AnyOf result after " + (now - startTime) + "ms: " + value);
        });

        return 0;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy");
        try {
            mExecutorService.shutdown();
            if (!mExecutorService.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                Log.e(LOG_TAG, "Cannot shutdown executor service");
                mExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Log.w(LOG_TAG, "Cannot shutdown executor service", e);
            mExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private Promise<Integer> action1(int value) {
        Log.i(LOG_TAG, "Action 1: " + value);
        Promise<Integer> promise = new Promise<>();
        mHandler.postDelayed(() -> { promise.complete(value + 1); }, 1000);
        return promise;
    }

    private Promise<Integer> action2(int value, Throwable exception) {
        Log.i(LOG_TAG, "Action 2: " + value);
        Promise<Integer> promise = new Promise<>();
        mHandler.postDelayed(() -> { promise.complete(value + 2); }, 1000);
        return promise;
    }

    private Promise<Integer> action3(int value) {
        Log.i(LOG_TAG, "Action 3: " + value);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        Promise<Integer> promise = new Promise<>(value + 3);
        return promise;
    }

    private Promise<Integer> action4(int value) {
        Log.i(LOG_TAG, "Action 4: " + value);
        Promise<Integer> promise = new Promise<>();
        mHandler.postDelayed(() -> { promise.complete(value + 4); }, 1000);
        return promise;
    }
}
