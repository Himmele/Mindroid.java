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

import mindroid.app.Service;
import mindroid.content.Intent;
import mindroid.os.Bundle;
import mindroid.os.IBinder;
import mindroid.os.RemoteException;
import mindroid.util.Log;

public class You extends Service {
    private static final String LOG_TAG = "Rogerian therapy";

    @Override
    public void onCreate() {
        Bundle extras = new Bundle();
        // extras.putString("scheme", "xmlrpc");

        Eliza eliza = new Eliza(this, extras);
        Log.d(LOG_TAG, "You: Hello");
        try {
            Log.d(LOG_TAG, "Eliza: " + eliza.ask1("Hello"));

            Log.d(LOG_TAG, "You: Well...");
            eliza.ask2("Well...").then(reply -> {
                Log.d(LOG_TAG, "Eliza: " + reply);

                Log.d(LOG_TAG, "You: What is 1 + 1?");
                try {
                    eliza.ask3("What is 1 + 1?", new Eliza.Listener() {
                        @Override
                        public void onReply(String reply) {
                            Log.d(LOG_TAG, "Eliza: " + reply);
                        }
                    });
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "You: Cannot talk to Eliza");
                }
            }).catchException(exception -> {
                Log.e(LOG_TAG, "You: Cannot talk to Eliza");
            });
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "You: Cannot talk to Eliza");
        }
    }

    @Override
    public void onDestroy() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
