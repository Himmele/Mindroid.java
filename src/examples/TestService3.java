package examples;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import mindroid.app.Service;
import mindroid.content.Intent;
import mindroid.os.Handler;
import mindroid.os.IBinder;
import mindroid.util.Log;
import mindroid.util.concurrent.Promise;

public class TestService3 extends Service {
    private static final String LOG_TAG = "TestService3";
    ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    Promise<Integer> mPromise1 = new Promise<>();
    Promise<Integer> mPromise2 = new Promise<>();

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
                e.printStackTrace();
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

        return 0;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy");
        try {
            mExecutorService.shutdown();
            mExecutorService.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.w(LOG_TAG, "Cannot shutdown executor service", e);
            mExecutorService.shutdownNow();
        }
    }
}
