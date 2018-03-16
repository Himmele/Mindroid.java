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

import mindroid.app.Service;
import mindroid.content.Intent;
import mindroid.os.Handler;
import mindroid.os.IBinder;
import mindroid.os.Message;

public class HandlerExample extends Service {
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
