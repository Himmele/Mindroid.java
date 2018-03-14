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

package examples.eliza;

import java.io.IOException;

import mindroid.app.Service;
import mindroid.content.Intent;
import mindroid.os.Handler;
import mindroid.os.IBinder;
import mindroid.os.RemoteException;
import mindroid.os.ServiceManager;
import mindroid.util.concurrent.Future;
import mindroid.util.concurrent.Promise;

public class ElizaService extends Service {
    private examples.eliza.util.Eliza mEliza;
    private IEliza.Stub mBinder;
    private Handler mHandler = new Handler();

    @Override
    public void onCreate() {
        if (mindroid.runtime.system.Runtime.getRuntime().getNodeId() == 1) {
            try {
                mEliza = new examples.eliza.util.Eliza();
            } catch (IOException e) {
                mEliza = null;
            }
            mBinder = new IEliza.Stub() {
                @Override
                public String ask1(String question) throws RemoteException {
                    return talk(question);
                }

                @Override
                public Future<String> ask2(String question) throws RemoteException {
                    Promise<String> promise = new Promise<>();
                    mHandler.postDelayed(() -> {
                        promise.complete(talk(question));
                    }, 1000);
                    return promise;
                }

                @Override
                public void ask3(String question, IElizaListener listener) throws RemoteException {
                    mHandler.postDelayed(() -> {
                        try {
                            listener.onReply(talk(question));
                        } catch (RemoteException e) {
                        }
                    }, 1000);
                }
            };
            ServiceManager.addService(Eliza.ELIZA, mBinder);
        }
    }

    @Override
    public void onDestroy() {
        if (mBinder != null) {
            ServiceManager.removeService(mBinder);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    public String talk(String input) {
        if (mEliza != null) {
            return mEliza.talk(input);
        } else {
            return "Out of Office";
        }
    }
}
