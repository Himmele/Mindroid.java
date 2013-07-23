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

package mindroid.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import mindroid.content.ComponentName;
import mindroid.content.Context;
import mindroid.content.Intent;
import mindroid.content.ServiceConnection;
import mindroid.content.SharedPreferences;
import mindroid.os.Environment;
import mindroid.os.HandlerThread;
import mindroid.os.IBinder;
import mindroid.os.IServiceManager;
import mindroid.os.Looper;
import mindroid.os.ServiceManager;
import mindroid.util.Log;

/**
 * Common implementation of Context API, which provides the base
 * context object for Activity and other application components.
 */
class ContextImpl extends Context {
	private final static String LOG_TAG = "ContextImpl";
	private static final HashMap sSharedPrefs = new HashMap();
    private final IServiceManager mServiceManager;
    private final HandlerThread mMainThread;
    private ComponentName mComponent;
    private HashMap mServiceConnections = new HashMap();

    ContextImpl(HandlerThread mainThread, ComponentName component) {
    	mServiceManager = ServiceManager.getIServiceManager();
    	mMainThread = mainThread;
    	mComponent = component;
    }
    
    public Looper getMainLooper() {
        return mMainThread.getLooper();
    }

    public String getPackageName() {
        if (mComponent != null) {
            return mComponent.getPackageName();
        }
        throw new RuntimeException("Not supported in system context");
    }
    
    public File getSharedPrefsFile(String name) {
        return makeFilename(getPreferencesDir(), name + ".xml");
    }
    
    public SharedPreferences getSharedPreferences(String name, int mode) {
    	SharedPreferencesImpl sp;
        synchronized (sSharedPrefs) {
            sp = (SharedPreferencesImpl) sSharedPrefs.get(name);
            if (sp == null) {
                File sharedPrefsFile = getSharedPrefsFile(name);
                sp = new SharedPreferencesImpl(sharedPrefsFile, mode);
                sSharedPrefs.put(name, sp);
                return sp;
            }
        }
        return sp;
    }
    
    public FileInputStream openFileInput(String name)
            throws FileNotFoundException {
    	File file = makeFilename(Environment.getDataDirectory(), name);
        return new FileInputStream(file);
    }
    
    public FileOutputStream openFileOutput(String name, int mode)
            throws FileNotFoundException {
    	final boolean append = (mode & MODE_APPEND) != 0;
        File file = makeFilename(Environment.getDataDirectory(), name);
        try {
            FileOutputStream fos = new FileOutputStream(file, append);
            return fos;
        } catch (FileNotFoundException e) {
        }
        
        return null;
    }

    public IBinder getSystemService(String name) {
		if (name != null) {
			return ServiceManager.getSystemService(name);
		} else {
			return null;
		}
	}
    
    public ComponentName startService(Intent service) {
    	if (service != null) {
    		return mServiceManager.startService(mComponent, service);
    	} else {
    		return null;
    	}
    }
    
    public boolean stopService(Intent service) {
    	if (service != null) {
    		return mServiceManager.stopService(mComponent, service);
    	} else {
    		return false;
    	}
    }
    
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
    	if (service != null && conn != null) {
			if (mServiceConnections.containsKey(conn)) {
				return true;
			}
    		mServiceConnections.put(conn, service);
    		return mServiceManager.bindService(mComponent, service, conn, flags);
    	} else {
    		return false;
    	}
    }
    
    public void unbindService(ServiceConnection conn) {
    	if (conn != null) {
    		if (mServiceConnections.containsKey(conn)) {
    			Intent service = (Intent) mServiceConnections.get(conn);
    			mServiceConnections.remove(conn);
    			mServiceManager.unbindService(mComponent, service, conn);
    		}
    	}
    }
    
    private File getPreferencesDir() {
    	return Environment.getPreferencesDirectory();
    }
    
    private File makeFilename(File baseDir, String name) {
        if (name.indexOf(File.separatorChar) < 0) {
            return new File(baseDir, name);
        }
        throw new IllegalArgumentException("File " + name + " contains a path separator");
    }
    
    void cleanup() {
    	Iterator itr = mServiceConnections.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry pair = (Map.Entry) itr.next();
            ServiceConnection conn = (ServiceConnection) pair.getKey();
            Intent service = (Intent) pair.getValue();
            unbindService(conn);
            Log.w(LOG_TAG, "Service " + mComponent + " is leaking a ServiceConnection to " + service.getComponent());
        }
    }
}
