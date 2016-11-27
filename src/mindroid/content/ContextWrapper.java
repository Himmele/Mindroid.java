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

package mindroid.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import mindroid.content.pm.PackageManager;
import mindroid.os.IBinder;
import mindroid.os.Looper;

/**
 * Proxying implementation of Context that simply delegates all of its calls to another Context. Can
 * be subclassed to modify behavior without changing the original Context.
 */
public class ContextWrapper extends Context {
    Context mBaseContext;

    public ContextWrapper(Context baseContext) {
        mBaseContext = baseContext;
    }

    /**
     * Set the base context for this ContextWrapper. All calls will then be delegated to the base
     * context. Throws IllegalStateException if a base context has already been set.
     * 
     * @param baseContext The new base context for this wrapper.
     */
    protected void attachBaseContext(Context baseContext) {
        if (mBaseContext != null) {
            throw new IllegalStateException("Base context already set");
        }
        mBaseContext = baseContext;
    }

    /**
     * @return the base context as set by the constructor or setBaseContext
     */
    public Context getBaseContext() {
        return mBaseContext;
    }

    public PackageManager getPackageManager() {
        return mBaseContext.getPackageManager();
    }

    public Looper getMainLooper() {
        return mBaseContext.getMainLooper();
    }

    public String getPackageName() {
        return mBaseContext.getPackageName();
    }

    public File getSharedPrefsFile(String name) {
        return mBaseContext.getSharedPrefsFile(name);
    }

    public SharedPreferences getSharedPreferences(String name, int mode) {
        return mBaseContext.getSharedPreferences(name, mode);
    }

    public FileInputStream openFileInput(String name) throws FileNotFoundException {
        return mBaseContext.openFileInput(name);
    }

    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        return mBaseContext.openFileOutput(name, mode);
    }

    public boolean deleteFile(String name) {
        return mBaseContext.deleteFile(name);
    }

    public File getFilesDir() {
        return mBaseContext.getFilesDir();
    }

    public IBinder getSystemService(String name) {
        return mBaseContext.getSystemService(name);
    }

    public ComponentName startService(Intent service) {
        return mBaseContext.startService(service);
    }

    public boolean stopService(Intent service) {
        return mBaseContext.stopService(service);
    }

    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        return mBaseContext.bindService(service, conn, flags);
    }

    public void unbindService(ServiceConnection conn) {
        mBaseContext.unbindService(conn);
    }
}
