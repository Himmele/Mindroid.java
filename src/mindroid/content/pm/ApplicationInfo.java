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

package mindroid.content.pm;

/**
 * Information you can retrieve about a particular application. This corresponds to information
 * collected from the MindroidManifest.xml's &lt;application&gt; tag.
 */
public class ApplicationInfo {

    /**
     * Public name of this item. From the "mindroid:name" attribute.
     */
    public String name;

    /**
     * Name of the package that this item is in.
     */
    public String packageName;

    /**
     * The name of the process this application should run in. From the "process" attribute or, if
     * not set, the same as <var>packageName</var>.
     */
    public String processName;

    /**
     * The app's declared version code.
     * 
     * @hide
     */
    public int versionCode;

    /**
     * When false, indicates that all components within this application are considered disabled,
     * regardless of their individually set enabled status.
     */
    public boolean enabled = true;

    /**
     * Paths to all shared libraries this application is linked against.
     */
    public String[] libraries = null;

    public String[] permissions = null;

    public String fileName = null;

    public ApplicationInfo() {
    }
}
