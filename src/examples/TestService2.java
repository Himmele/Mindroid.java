package examples;

import mindroid.app.Service;
import mindroid.content.Intent;
import mindroid.os.Binder;
import mindroid.os.IBinder;
import mindroid.util.Log;

public class TestService2 extends Service {
	private static final String LOG_TAG = "TestService2";

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
