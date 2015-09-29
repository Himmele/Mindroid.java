package examples;

import mindroid.app.Service;
import mindroid.content.ComponentName;
import mindroid.content.Intent;
import mindroid.content.ServiceConnection;
import mindroid.os.Handler;
import mindroid.os.IBinder;
import mindroid.util.Log;

public class TestService1 extends Service {
	private static final String LOG_TAG = "TestService1";
	private final Handler mHandler = new Handler();

	public void onCreate() {
		final Intent intent = new Intent();
		intent.setClassName("tests", "TestService2");
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
