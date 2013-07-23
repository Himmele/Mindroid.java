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
 * Base class for a remotable object, the core part of a lightweight
 * remote procedure call mechanism defined by {@link IBinder}.
 * This class is an implementation of IBinder that provides
 * the standard support creating a local implementation of such an object.
 * 
 * You can derive directly from Binder to implement your own custom RPC
 * protocol or simply instantiate a raw Binder object directly to use as a
 * token that can be shared across processes.
 * 
 * @see IBinder
 */
public class Binder extends Handler implements IBinder {
	private IInterface mOwner;
    private String mDescriptor;
    private Thread mThread;
    
    public Binder() {
    	super();
    }
    
    /** @hide */
    public Binder(Looper looper) {
    	super(looper);
    }
    
    /**
     * Convenience method for associating a specific interface with the Binder.
     * After calling, queryInterface() will be implemented for you
     * to return the given owner IInterface when the corresponding
     * descriptor is requested.
     */
    public void attachInterface(IInterface owner, String descriptor) {
        mOwner = owner;
        mDescriptor = descriptor;
        mThread = Thread.currentThread();
    }
    
    public void attachInterface(IInterface owner, String descriptor, Thread thread) {
        mOwner = owner;
        mDescriptor = descriptor;
        mThread = thread;
    }
	
    /**
     * Use information supplied to attachInterface() to return the
     * associated IInterface if it matches the requested
     * descriptor.
     */
	public IInterface queryInterface(String descriptor) {
        if (mDescriptor.equals(descriptor)) {
            return mOwner;
        }
        return null;
    }
	
	/**
     * Default implementation returns an empty interface name.
     */
	public String getInterfaceDescriptor() {
		return mDescriptor;
	}
	
	public boolean checkThread() {
		return (mThread == Thread.currentThread());
    }
}
