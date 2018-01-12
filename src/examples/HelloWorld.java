package examples;

import mindroid.app.Service;
import mindroid.content.Intent;
import mindroid.os.Handler;
import mindroid.os.IBinder;
import mindroid.os.Message;
import mindroid.util.Log;

public class HelloWorld extends Service {
    private static final String LOG_TAG = "HelloWorld";
    private static final int SAY_HELLO = 0;
    private static final int SAY_WORLD = 1;

    private final Handler mHelloHandler = new Handler() {
        public void handleMessage(Message message) {
            switch (message.what) {
            case SAY_HELLO:
                System.out.print("Hello ");
                mWorldHandler.obtainMessage(SAY_WORLD).sendToTarget();
                break;
            }
        }
    };

    private final Handler mWorldHandler = new Handler() {
        public void handleMessage(Message message) {
            switch (message.what) {
            case SAY_WORLD:
                System.out.println("World!");
                Message hello = mHelloHandler.obtainMessage(SAY_HELLO);
                mHelloHandler.sendMessageDelayed(hello, 1000);
                break;
            }
        }
    };

    public void onCreate() {
        mHelloHandler.obtainMessage(SAY_HELLO).sendToTarget();
    }

    public void onDestroy() {
        mHelloHandler.removeMessages(SAY_HELLO);
        mWorldHandler.removeMessages(SAY_WORLD);
    }

    public IBinder onBind(Intent intent) {
        return null;
    }
}
