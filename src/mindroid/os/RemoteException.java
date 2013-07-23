/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (C) 2013 Daniel Himmelein
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
 * Parent exception for all Binder remote-invocation errors
 */
public class RemoteException extends RuntimeException {
    public RemoteException() {
        super();
    }

    public RemoteException(String message) {
        super(message);
    }
    
    public RemoteException(String message, Throwable cause) {
        super(message, cause);
    }

    public RemoteException(Exception cause) {
        super(cause);
    }
}
