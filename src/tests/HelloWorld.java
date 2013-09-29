package tests;

import mindroid.app.Service;
import mindroid.content.Intent;
import mindroid.os.Handler;
import mindroid.os.IBinder;
import mindroid.os.Message;
import mindroid.util.Log;

public class HelloWorld extends Service {
	private static final String LOG_TAG = "HelloWorld";
	private static final int HELLO = 0;
	private static final int WORLD = 1;
	
	private final Handler mHelloHandler = new Handler() {
		public void handleMessage(Message message) {
			switch (message.what) {
			case HELLO:
				System.out.print("Hello ");
				mWorldHandler.obtainMessage(WORLD).sendToTarget();
			}
		}
	};
	
	private final Handler mWorldHandler = new Handler() {
		public void handleMessage(Message message) {
			switch (message.what) {
			case WORLD:
				System.out.println("World!");
				Message hello = mHelloHandler.obtainMessage(HELLO);
				mHelloHandler.sendMessageDelayed(hello, 1000);
			}
		}
	};

	public void onCreate() {
		mHelloHandler.obtainMessage(HELLO).sendToTarget();
	}
	
	public void onDestroy() {
		mHelloHandler.removeMessages(HELLO);
		mWorldHandler.removeMessages(WORLD);
	}

	public IBinder onBind(Intent intent) {
		return null;
	}
}
