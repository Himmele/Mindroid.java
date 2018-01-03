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
 * Overall information about the contents of a package. This corresponds to all of the information
 * collected from MindroidManifest.xml.
 */
public class PackageInfo {
    /**
     * The name of this package. From the &lt;manifest&gt; tag's "name" attribute.
     */
    public String packageName;

    /**
     * The version number of this package, as specified by the &lt;manifest&gt; tag's
     * {@link MindroidManifest_versionCode versionCode} attribute.
     */
    public int versionCode;

    /**
     * The version name of this package, as specified by the &lt;manifest&gt; tag's
     * {@link MindroidManifest_versionName versionName} attribute.
     */
    public String versionName;

    /**
     * Information collected from the &lt;application&gt; tag, or null if there was none.
     */
    public ApplicationInfo applicationInfo;

    /**
     * Array of all {@link MindroidManifestService &lt;service&gt;} tags included under
     * &lt;application&gt;, or null if there were none. This is only filled in if the flag
     * {@link PackageManager#GET_SERVICES} was set.
     */
    public ServiceInfo[] services;

    /**
     * Array of all {@link MindroidManifestService &lt;uses-permission&gt;} tags included
     * under &lt;manifest&gt;, or null if there were none.
     * This is only filled in if the flag {@link PackageManager#GET_PERMISSIONS} was set.
     * This list includes all permissions requested, even those that were not granted
     * or known by the system at install time.
     */
    public String[] permissions;

    public PackageInfo() {
    }
}
