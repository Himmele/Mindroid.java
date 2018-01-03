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

/**
 * Identifier for a specific application component {@link mindroid.app.Service}, Two pieces of
 * information, encapsulated here, are required to identify a component: the package (a String) it
 * exists in, and the class (a String) name inside of that package.
 * 
 */
public final class ComponentName {
    private final String mPackage;
    private final String mClass;

    /**
     * Create a new component identifier.
     * 
     * @param pkg The name of the package that the component exists in. Can not be null.
     * @param cls The name of the class inside of <var>pkg</var> that implements the component. Can
     * not be null.
     */
    public ComponentName(String pkg, String cls) {
        if (pkg == null) throw new NullPointerException("package name is null");
        if (cls == null) throw new NullPointerException("class name is null");
        mPackage = pkg;
        mClass = cls;
    }

    /**
     * Create a new component identifier from a Context and class name.
     * 
     * @param pkg A Context for the package implementing the component, from which the actual
     * package name will be retrieved.
     * @param cls The name of the class inside of <var>pkg</var> that implements the component.
     */
    public ComponentName(Context pkg, String cls) {
        if (cls == null) throw new NullPointerException("class name is null");
        mPackage = pkg.getPackageName();
        mClass = cls;
    }

    /**
     * Create a new component identifier from a Context and Class object.
     * 
     * @param pkg A Context for the package implementing the component, from which the actual
     * package name will be retrieved.
     * @param cls The Class object of the desired component, from which the actual class name will
     * be retrieved.
     */
    public ComponentName(Context pkg, Class<?> cls) {
        mPackage = pkg.getPackageName();
        mClass = cls.getName();
    }

    /**
     * Return the package name of this component.
     */
    public String getPackageName() {
        return mPackage;
    }

    /**
     * Return the class name of this component.
     */
    public String getClassName() {
        return mClass;
    }

    /**
     * Return string representation of this class without the class's name as a prefix.
     */
    public String toShortString() {
        return "{" + mPackage + "/" + mClass + "}";
    }

    public String toString() {
        return "ComponentInfo{" + mPackage + "/" + mClass + "}";
    }

    public boolean equals(Object obj) {
        try {
            if (obj != null) {
                ComponentName other = (ComponentName) obj;
                // Note: no null checks, because mPackage and mClass can
                // never be null.
                return mPackage.equals(other.mPackage) && mClass.equals(other.mClass);
            }
        } catch (ClassCastException e) {
        }
        return false;
    }

    public int hashCode() {
        return mPackage.hashCode() + mClass.hashCode();
    }
}
