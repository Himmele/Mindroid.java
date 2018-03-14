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

import java.util.Arrays;
import mindroid.app.Service;
import mindroid.content.Intent;
import mindroid.os.AsyncTask;
import mindroid.os.IBinder;
import mindroid.util.Log;

public class AsyncTaskExample extends Service {
    private static final String LOG_TAG = "AsyncTaskExample";

    public void onCreate() {
        Log.i(LOG_TAG, "onCreate");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        AsyncTask<Integer, Integer, Integer> asyncTask = new AsyncTask<Integer, Integer, Integer>() {
            @Override
            protected void onPreExecute() {
                Log.i(LOG_TAG, "onPreExecute");
            }

            @Override
            protected Integer doInBackground(Integer... params) {
                Log.i(LOG_TAG, "doInBackground: " + Arrays.toString(params));
                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Log.e(LOG_TAG, e.getMessage(), e);
                        }
                        publishProgress(params[i]);
                    }
                }
                return 42;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                Log.i(LOG_TAG, "onProgressUpdate: " + Arrays.toString(values));
            }

            @Override
            protected void onPostExecute(Integer result) {
                Log.i(LOG_TAG, "onPostExecute: " + result);
            }

            @Override
            protected void onCancelled(Integer result) {
                Log.i(LOG_TAG, "onCancelled: " + result);
            }
        };
        asyncTask.execute(1, 2, 3);

        return 0;
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy");
    }
}
