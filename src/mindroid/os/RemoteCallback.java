/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2016 Daniel Himmelein
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

package mindroid.os;

/**
 * @hide
 */
public final class RemoteCallback {

    public interface OnResultListener {
        public void onResult(Bundle result);
    }

    private final OnResultListener mListener;
    private final Handler mHandler;
    private final IRemoteCallback mCallback;

    public RemoteCallback(OnResultListener listener) {
        this(listener, null);
    }

    public RemoteCallback(OnResultListener listener, Handler handler) {
        if (listener == null) {
            throw new NullPointerException("Listener cannot be null");
        }
        mListener = listener;
        mHandler = handler;
        mCallback = new IRemoteCallback.Stub() {
            public void sendResult(Bundle data) {
                RemoteCallback.this.sendResult(data);
            }
        };
    }

    private void sendResult(final Bundle result) {
        if (mHandler != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onResult(result);
                }
            });
        } else {
            mListener.onResult(result);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        try {
            return mCallback.asBinder().equals(((RemoteCallback) other).mCallback.asBinder());
        } catch (ClassCastException e) {
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mCallback.asBinder().hashCode();
    }

    public IRemoteCallback asInterface() {
        return mCallback;
    }
}
