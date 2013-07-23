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

/**
 * Provides access to environment variables.
 */
public class Environment {
    private static File ROOT_DIRECTORY = new File(".");
    private static File DATA_DIRECTORY = new File(ROOT_DIRECTORY, "data");
    private static File APPS_DIRECTORY = new File(ROOT_DIRECTORY, "apps");
    private static File PREFERENCES_DIRECTORY = new File(ROOT_DIRECTORY, "prefs");
    
    /**
     * Sets the Mindroid root directory.
     * 
     * @hide
     */
    public static void setRootDirectory(String rootDirectory) {
        ROOT_DIRECTORY = new File(rootDirectory);
        DATA_DIRECTORY = new File(ROOT_DIRECTORY, "data");
        APPS_DIRECTORY = new File(ROOT_DIRECTORY, "apps");
        PREFERENCES_DIRECTORY = new File(ROOT_DIRECTORY, "prefs");
    }

    /**
     * Gets the Mindroid root directory.
     */
    public static File getRootDirectory() {
        return ROOT_DIRECTORY;
    }

    /**
     * Gets the Mindroid data directory.
     */
    public static File getDataDirectory() {
        return DATA_DIRECTORY;
    }
    
    /**
     * Gets the Mindroid apps directory.
     */
    public static File getAppsDirectory() {
        return APPS_DIRECTORY;
    }
    
    /**
     * Gets the Mindroid preferences directory.
     */
    public static File getPreferencesDirectory() {
        return PREFERENCES_DIRECTORY;
    }
}
