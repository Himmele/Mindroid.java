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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import mindroid.content.Context;
import mindroid.os.Bundle;
import mindroid.os.IBinder;
import mindroid.os.RemoteException;
import mindroid.util.concurrent.Future;

public class Eliza {
    public static final URI ELIZA = URI.create("mindroid://eliza");

    private IEliza mService;
    private List<ListenerWrapper> mListeners = new ArrayList<>();

    public interface Listener {
        public void onReply(final String reply);
    }

    private class ListenerWrapper extends IElizaListener.Stub {
        private Listener mListener;

        ListenerWrapper(Listener listener) {
            mListener = listener;
        }

        @Override
        public void onReply(String reply) throws RemoteException {
            mListeners.remove(this);
            mListener.onReply(reply);
        }
    }

    public Eliza(Context context) {
        IBinder binder = null;
        for (int i = 0; i < 10; i++) {
            if ((binder = context.getSystemService(ELIZA)) != null) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        mService = IEliza.Stub.asInterface(binder);
    }

    public Eliza(Context context, Bundle extras) {
        URI uri = null;
        if (extras != null) {
            String scheme = extras.getString("scheme");
            if (scheme != null) {
                try {
                    uri = URI.create(scheme + "://" + ELIZA.getAuthority());
                } catch (IllegalArgumentException e) {
                    mService = null;
                }
            } else {
                uri = ELIZA;
            }
        } else {
            uri = ELIZA;
        }

        for (int i = 0; i < 10; i++) {
            if (context.getSystemService(uri) != null) {
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        mService = IEliza.Stub.asInterface(context.getSystemService(uri));
    }

    public String ask1(String question) throws RemoteException {
        if (mService == null) {
            throw new RemoteException();
        }

        return mService.ask1(question);
    }

    public Future<String> ask2(String question) throws RemoteException {
        if (mService == null) {
            throw new RemoteException();
        }

        return mService.ask2(question);
    }

    public void ask3(String question, Listener listener) throws RemoteException {
        if (mService == null) {
            throw new RemoteException();
        }

        ListenerWrapper wrapper = new ListenerWrapper(listener);
        mListeners.add(wrapper);
        try {
            mService.ask3(question, wrapper);
        } catch (RemoteException e) {
            mListeners.remove(wrapper);
            throw e;
        }
    }
}
