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
import java.net.URI;
import mindroid.content.SharedPreferences;
import mindroid.content.pm.PackageManager;
import mindroid.os.IBinder;
import mindroid.os.Looper;
import mindroid.util.concurrent.Future;

/**
 * Interface to global information about an application environment. This is an class whose
 * implementation is provided by the Mindroid system. It allows access to application-specific
 * resources and classes, as well as up-calls for application-level operations such as launching
 * services, etc.
 */
public abstract class Context {
    public static final URI SERVICE_MANAGER = URI.create("mindroid://serviceManager");
    public static final URI PACKAGE_MANAGER = URI.create("mindroid://packageManager");
    public static final URI PACKAGE_INSTALLER = URI.create("mindroid://packageInstaller");
    public static final URI LOGGER_SERVICE = URI.create("mindroid://logger");
    public static final URI CONSOLE_SERVICE = URI.create("mindroid://console");
    public static final URI CLOCK_SERVICE = URI.create("mindroid://clockService");
    public static final URI ALARM_MANAGER = URI.create("mindroid://alarmManager");
    public static final URI POWER_MANAGER = URI.create("mindroid://powerManager");
    public static final URI TELEPHONY_SERVICE = URI.create("mindroid://telephonyService");
    public static final URI CONNECTION_SERVICE = URI.create("mindroid://connectionService");
    public static final URI LOCATION_SERVICE = URI.create("mindroid://locationService");
    public static final URI MEDIA_PLAYER_SERVICE = URI.create("mindroid://mediaPlayerService");
    public static final URI MESSAGE_BROKER = URI.create("mindroid://messageBroker");
    public static final URI SUPERVISOR_SERVICE = URI.create("mindroid://supervisorService");

    /**
     * File creation mode: the default mode.
     */
    public static final int MODE_PRIVATE = 0x0000;

    /**
     * File creation mode: for use with {@link #openFileOutput}, if the file already exists then
     * write data to the end of the existing file instead of erasing it.
     * 
     * @see #openFileOutput
     */
    public static final int MODE_APPEND = 0x8000;

    /** Return PackageManager instance to find global package information. */
    public abstract PackageManager getPackageManager();

    /**
     * Return the Looper for the main thread of the current process. This is the thread used to
     * dispatch calls to application components (services, etc).
     */
    public abstract Looper getMainLooper();

    /** Return the name of this application's package. */
    public abstract String getPackageName();

    /**
     * {@hide} Return the full path to the shared prefs file for the given prefs group name.
     * 
     * <p>
     * Note: this is not generally useful for applications, since they should not be directly
     * accessing the file system.
     */
    public abstract File getSharedPrefsFile(String name);

    /**
     * Retrieve and hold the contents of the preferences file 'name', returning a SharedPreferences
     * through which you can retrieve and modify its values. Only one instance of the
     * SharedPreferences object is returned to any callers for the same name, meaning they will see
     * each other's edits as soon as they are made.
     * 
     * @param name Desired preferences file. If a preferences file by this name does not exist, it
     * will be created when you retrieve an editor (SharedPreferences.edit()) and then commit
     * changes (Editor.commit()).
     * @param mode Operating mode. Use 0 or {@link #MODE_PRIVATE} for the default operation.
     * 
     * @return Returns the single SharedPreferences instance that can be used to retrieve and modify
     * the preference values.
     */
    public abstract SharedPreferences getSharedPreferences(String name, int mode);

    /**
     * Open a file associated with this Context's application package for reading.
     * 
     * @param name The name of the file to open; can not contain path separators.
     * 
     * @return FileInputStream Resulting input stream.
     * 
     * @see #openFileOutput
     * @see java.io.FileInputStream#FileInputStream(String)
     */
    public abstract FileInputStream openFileInput(String name) throws FileNotFoundException;

    /**
     * Open a private file associated with this Context's application package for writing. Creates
     * the file if it doesn't already exist.
     * 
     * @param name The name of the file to open; can not contain path separators.
     * @param mode Operating mode. Use 0 for the default operation, or {@link #MODE_APPEND} to
     * append to an existing file.
     * 
     * @return FileOutputStream Resulting output stream.
     * 
     * @see #MODE_APPEND
     * @see #openFileInput
     * @see java.io.FileOutputStream#FileOutputStream(String)
     */
    public abstract FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException;

    /**
     * Delete the given private file associated with this Context's application package.
     * 
     * @param name The name of the file to delete; can not contain path separators.
     * 
     * @return {@code true} if the file was successfully deleted; else {@code false}.
     * 
     * @see #openFileInput
     * @see #openFileOutput
     * @see #fileList
     * @see java.io.File#delete()
     */
    public abstract boolean deleteFile(String name);

    /**
     * Returns the absolute path to the directory on the filesystem where files created with
     * {@link #openFileOutput} are stored.
     * 
     * <p>
     * No permissions are required to read or write to the returned path, since this path is
     * internal storage.
     * 
     * @return The path of the directory holding application files.
     * 
     * @see #openFileOutput
     */
    public abstract File getFilesDir();

    /**
     * Return the handle to a system-level service by name. The class of the returned object varies
     * by the requested name.
     * 
     * @param name Name of the system service.
     */
    public abstract IBinder getSystemService(URI name);

    /**
     * Request that a given application service be started. The Intent must contain the complete
     * class name of a specific service implementation to start. If this service is not already
     * running, it will be instantiated and started (creating a process for it if needed); if it is
     * running then it remains running.
     * 
     * <p>
     * Every call to this method will result in a corresponding call to the target service's
     * {@link mindroid.app.Service#onStartCommand} method, with the <var>intent</var> given here.
     * This provides a convenient way to submit jobs to a service without having to bind and call on
     * to its interface.
     * 
     * <p>
     * Using startService() overrides the default service lifetime that is managed by
     * {@link #bindService}: it requires the service to remain running until {@link #stopService} is
     * called, regardless of whether any clients are connected to it. Note that calls to
     * startService() are not nesting: no matter how many times you call startService(), a single
     * call to {@link #stopService} will stop it.
     * 
     * <p>
     * The system attempts to keep running services around as much as possible.
     * 
     * @param service Identifies the service to be started. The Intent must specify an explicit
     * component name to start. Additional values may be included in the Intent extras to supply
     * arguments along with this specific start call.
     * 
     * @return If the service is being started or is already running, the {@link ComponentName} of
     * the actual service that was started is returned; else if the service does not exist null is
     * returned.
     * 
     * @see #stopService
     * @see #bindService
     */
    public abstract Future<ComponentName> startService(Intent service);

    /**
     * Request that a given application service be stopped. If the service is not running, nothing
     * happens. Otherwise it is stopped. Note that calls to startService() are not counted -- this
     * stops the service no matter how many times it was started.
     * 
     * <p>
     * Note that if a stopped service still has {@link ServiceConnection} objects bound to it, it
     * will not be destroyed until all of these bindings are removed. See the
     * {@link mindroid.app.Service} documentation for more details on a service's lifecycle.
     * 
     * @param service Description of the service to be stopped. The Intent must specify either an
     * explicit component name to start.
     * 
     * @return If there is a service matching the given Intent that is already running, then it is
     * stopped and true is returned; else false is returned.
     * 
     * @see #startService
     */
    public abstract Future<Boolean> stopService(Intent service);

    /**
     * Connect to an application service, creating it if needed. This defines a dependency between
     * your service and the requested service. The given <var>conn</var> will receive the service
     * object when it is created and be told if it dies and restarts. The service will be considered
     * required by the system only for as long as the calling context exists. For example, if this
     * Context is an Service that is stopped, the service will not be required to continue running
     * until the Service is recreated.
     * 
     * @param service Identifies the service to connect to. The Intent must specify an explicit
     * component name.
     * @param conn Receives information as the service is started and stopped. This must be a valid
     * ServiceConnection object; it must not be null.
     * @param flags Currently not used. Should be 0.
     * @return If you have successfully bound to the service, true is returned; false is returned if
     * the connection is not made so you will not receive the service object.
     * 
     * @see #unbindService
     * @see #startService
     */
    public abstract Future<Boolean> bindService(Intent service, ServiceConnection conn, int flags);

    /**
     * Disconnect from an application service. You will no longer receive calls as the service is
     * restarted, and the service is now allowed to stop at any time.
     * 
     * @param conn The connection interface previously supplied to bindService(). This parameter
     * must not be null.
     * 
     * @see #bindService
     */
    public abstract void unbindService(ServiceConnection conn);
}
