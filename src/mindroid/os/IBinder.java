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
 * Base interface for a remotable object, the core part of a lightweight
 * remote procedure call mechanism designed for high performance when
 * performing in-process and cross-process calls.  This
 * interface describes the abstract protocol for interacting with a
 * remotable object.  Do not implement this interface directly, instead
 * extend from {@link Binder}.
 * 
 * @see Binder
 */
public interface IBinder {
	public static final int FLAG_ONEWAY = 0x00000001;
	
	/**
     * Get the canonical name of the interface supported by this binder.
     */
	public String getInterfaceDescriptor() throws RemoteException;
	
	public IInterface queryLocalInterface(String descriptor);
	
	public Object transact(int what, int flags) throws RemoteException;
	public Object transact(int what, Object obj, int flags) throws RemoteException;
	public Object transact(int what, int arg1, int arg2, int flags) throws RemoteException;
	public Object transact(int what, int arg1, int arg2, Object obj, int flags) throws RemoteException;
	public Object transact(int what, Bundle bundle, int flags) throws RemoteException;

	public boolean runsOnSameThread();
}
