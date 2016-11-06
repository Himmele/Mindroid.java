/*
 * Copyright (C) 2007 The Android Open Source Project
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

import java.io.File;
import java.util.HashMap;
import mindroid.app.SharedPreferencesImpl;
import mindroid.content.SharedPreferences;

/**
 * Provides access to environment variables.
 */
public class Environment {
	private static File ROOT_DIRECTORY = new File(".");
	private static File APPS_DIRECTORY = new File(ROOT_DIRECTORY, "apps");
	private static File DATA_DIRECTORY = new File(ROOT_DIRECTORY, "data");
	private static File PREFERENCES_DIRECTORY = new File(ROOT_DIRECTORY, "prefs");
	private static File LOG_DIRECTORY = new File(ROOT_DIRECTORY, "logs");
	private static final HashMap sSharedPrefs = new HashMap();

	/**
	 * Sets the Mindroid root directory.
	 * 
	 * @hide
	 */
	public static void setRootDirectory(String rootDirectory) {
		ROOT_DIRECTORY = new File(rootDirectory);
		APPS_DIRECTORY = new File(ROOT_DIRECTORY, "apps");
		DATA_DIRECTORY = new File(ROOT_DIRECTORY, "data");
		PREFERENCES_DIRECTORY = new File(ROOT_DIRECTORY, "prefs");
		LOG_DIRECTORY = new File(ROOT_DIRECTORY, "logs");
	}

	/**
	 * Gets the Mindroid root directory.
	 */
	public static File getRootDirectory() {
		return ROOT_DIRECTORY;
	}

	/**
	 * Gets the Mindroid apps directory.
	 */
	public static File getAppsDirectory() {
		return APPS_DIRECTORY;
	}

	/**
	 * Gets the Mindroid data directory.
	 */
	public static File getDataDirectory() {
		return DATA_DIRECTORY;
	}

	/**
	 * Gets the Mindroid preferences directory.
	 */
	public static File getPreferencesDirectory() {
		return PREFERENCES_DIRECTORY;
	}

	/**
	 * Gets the Mindroid log directory.
	 */
	public static File getLogDirectory() {
		return LOG_DIRECTORY;
	}

	/**
	 * Sets the Mindroid log directory.
	 */
	public static void setLogDirectory(String directory) {
		LOG_DIRECTORY = new File(directory);
	}
	
	/**
     * Retrieve and hold the contents of the preferences file 'fileName', returning a SharedPreferences
     * through which you can retrieve and modify its values. Only one instance of the
     * SharedPreferences object is returned to any callers for the same name, meaning they will see
     * each other's edits as soon as they are made.
     * 
     * @param baseDir The base directory of the preferences file.
     * @param fileName Desired preferences file name. If a preferences file by this name does not exist, it
     * will be created when you retrieve an editor (SharedPreferences.edit()) and then commit
     * changes (Editor.commit()).
     * @param mode Operating mode. Use 0 or {@link #MODE_PRIVATE} for the default operation.
     * 
     * @return Returns the single SharedPreferences instance that can be used to retrieve and modify
     * the preference values.
     */
    public static SharedPreferences getSharedPreferences(File baseDir, String fileName, int mode) {
        File sharedPrefsFile = new File(baseDir, fileName);
        return getSharedPreferences(sharedPrefsFile, mode);
    }
    
    /**
     * Retrieve and hold the contents of the preferences file 'fileName', returning a SharedPreferences
     * through which you can retrieve and modify its values. Only one instance of the
     * SharedPreferences object is returned to any callers for the same name, meaning they will see
     * each other's edits as soon as they are made.
     * 
     * @param sharedPrefsFile Desired preferences file. If a preferences file by this name does not exist, it
     * will be created when you retrieve an editor (SharedPreferences.edit()) and then commit
     * changes (Editor.commit()).
     * @param mode Operating mode. Use 0 or {@link #MODE_PRIVATE} for the default operation.
     * 
     * @return Returns the single SharedPreferences instance that can be used to retrieve and modify
     * the preference values.
     */
    public static SharedPreferences getSharedPreferences(File sharedPrefsFile, int mode) {
        SharedPreferencesImpl sp;
        synchronized (sSharedPrefs) {
            sp = (SharedPreferencesImpl) sSharedPrefs.get(sharedPrefsFile.getAbsolutePath());
            if (sp == null) {
                sp = new SharedPreferencesImpl(sharedPrefsFile, mode);
                sSharedPrefs.put(sharedPrefsFile.getAbsolutePath(), sp);
                return sp;
            }
        }
        return sp;
    }
}
