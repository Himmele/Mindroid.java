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

import java.util.ArrayList;
import mindroid.os.Bundle;

/**
 * An intent is an abstract description of an operation to be performed. It can be used with
 * {@link mindroid.content.Context#startService} or {@link mindroid.content.Context#bindService} to
 * communicate with a {@link mindroid.app.Service}.
 * 
 * <p>
 * An Intent provides a facility for performing late runtime binding between the code in different
 * applications. Its most significant use is in the launching of services, where it can be thought
 * of as the glue between services. It is basically a passive data structure holding an abstract
 * description of an action to be performed.
 * </p>
 * 
 */
public class Intent {
    private ComponentName mComponent;
    private Bundle mExtras;

    /**
     * Create an empty intent.
     */
    public Intent() {
    }

    /**
     * Copy constructor.
     */
    public Intent(Intent o) {
        this.mComponent = o.mComponent;
        if (o.mExtras != null) {
            this.mExtras = new Bundle(o.mExtras);
        }
    }

    public Object clone() {
        return new Intent(this);
    }

    /**
     * Returns true if an extra value is associated with the given name.
     * 
     * @param name the extra's name
     * @return true if the given extra is present.
     */
    public boolean hasExtra(String name) {
        return mExtras != null && mExtras.containsKey(name);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * @param defaultValue the value to be returned if no value of the desired type is stored with
     * the given name.
     * 
     * @return the value of an item that previously added with putExtra() or the default value if
     * none was found.
     * 
     * @see #putExtra(String, boolean)
     */
    public boolean getBooleanExtra(String name, boolean defaultValue) {
        return mExtras == null ? defaultValue : mExtras.getBoolean(name, defaultValue);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * @param defaultValue the value to be returned if no value of the desired type is stored with
     * the given name.
     * 
     * @return the value of an item that previously added with putExtra() or the default value if
     * none was found.
     * 
     * @see #putExtra(String, byte)
     */
    public byte getByteExtra(String name, byte defaultValue) {
        return mExtras == null ? defaultValue : mExtras.getByte(name, defaultValue);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * @param defaultValue the value to be returned if no value of the desired type is stored with
     * the given name.
     * 
     * @return the value of an item that previously added with putExtra() or the default value if
     * none was found.
     * 
     * @see #putExtra(String, short)
     */
    public short getShortExtra(String name, short defaultValue) {
        return mExtras == null ? defaultValue : mExtras.getShort(name, defaultValue);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * @param defaultValue the value to be returned if no value of the desired type is stored with
     * the given name.
     * 
     * @return the value of an item that previously added with putExtra() or the default value if
     * none was found.
     * 
     * @see #putExtra(String, char)
     */
    public char getCharExtra(String name, char defaultValue) {
        return mExtras == null ? defaultValue : mExtras.getChar(name, defaultValue);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * @param defaultValue the value to be returned if no value of the desired type is stored with
     * the given name.
     * 
     * @return the value of an item that previously added with putExtra() or the default value if
     * none was found.
     * 
     * @see #putExtra(String, int)
     */
    public int getIntExtra(String name, int defaultValue) {
        return mExtras == null ? defaultValue : mExtras.getInt(name, defaultValue);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * @param defaultValue the value to be returned if no value of the desired type is stored with
     * the given name.
     * 
     * @return the value of an item that previously added with putExtra() or the default value if
     * none was found.
     * 
     * @see #putExtra(String, long)
     */
    public long getLongExtra(String name, long defaultValue) {
        return mExtras == null ? defaultValue : mExtras.getLong(name, defaultValue);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * @param defaultValue the value to be returned if no value of the desired type is stored with
     * the given name.
     * 
     * @return the value of an item that previously added with putExtra(), or the default value if
     * no such item is present
     * 
     * @see #putExtra(String, float)
     */
    public float getFloatExtra(String name, float defaultValue) {
        return mExtras == null ? defaultValue : mExtras.getFloat(name, defaultValue);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * @param defaultValue the value to be returned if no value of the desired type is stored with
     * the given name.
     * 
     * @return the value of an item that previously added with putExtra() or the default value if
     * none was found.
     * 
     * @see #putExtra(String, double)
     */
    public double getDoubleExtra(String name, double defaultValue) {
        return mExtras == null ? defaultValue : mExtras.getDouble(name, defaultValue);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * 
     * @return the value of an item that previously added with putExtra() or null if no String value
     * was found.
     * 
     * @see #putExtra(String, String)
     */
    public String getStringExtra(String name) {
        return mExtras == null ? null : mExtras.getString(name);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * 
     * @return the value of an item that previously added with putExtra() or null if no boolean
     * array value was found.
     * 
     * @see #putExtra(String, boolean[])
     */
    public boolean[] getBooleanArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getBooleanArray(name);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * 
     * @return the value of an item that previously added with putExtra() or null if no byte array
     * value was found.
     * 
     * @see #putExtra(String, byte[])
     */
    public byte[] getByteArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getByteArray(name);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * 
     * @return the value of an item that previously added with putExtra() or null if no short array
     * value was found.
     * 
     * @see #putExtra(String, short[])
     */
    public short[] getShortArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getShortArray(name);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * 
     * @return the value of an item that previously added with putExtra() or null if no char array
     * value was found.
     * 
     * @see #putExtra(String, char[])
     */
    public char[] getCharArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getCharArray(name);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * 
     * @return the value of an item that previously added with putExtra() or null if no int array
     * value was found.
     * 
     * @see #putExtra(String, int[])
     */
    public int[] getIntArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getIntArray(name);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * 
     * @return the value of an item that previously added with putExtra() or null if no long array
     * value was found.
     * 
     * @see #putExtra(String, long[])
     */
    public long[] getLongArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getLongArray(name);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * 
     * @return the value of an item that previously added with putExtra() or null if no float array
     * value was found.
     * 
     * @see #putExtra(String, float[])
     */
    public float[] getFloatArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getFloatArray(name);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * 
     * @return the value of an item that previously added with putExtra() or null if no double array
     * value was found.
     * 
     * @see #putExtra(String, double[])
     */
    public double[] getDoubleArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getDoubleArray(name);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * 
     * @return the value of an item that previously added with putExtra() or null if no String array
     * value was found.
     * 
     * @see #putExtra(String, String[])
     */
    public String[] getStringArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getStringArray(name);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * 
     * @return the value of an item that previously added with putExtra() or null if no
     * ArrayList<Integer> value was found.
     * 
     * @see #putIntegerArrayListExtra(String, ArrayList)
     */
    public ArrayList getIntegerArrayListExtra(String name) {
        return mExtras == null ? null : mExtras.getIntegerArrayList(name);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * 
     * @return the value of an item that previously added with putExtra() or null if no
     * ArrayList<String> value was found.
     * 
     * @see #putStringArrayListExtra(String, ArrayList)
     */
    public ArrayList getStringArrayListExtra(String name) {
        return mExtras == null ? null : mExtras.getStringArrayList(name);
    }

    /**
     * Retrieve extended data from the intent.
     * 
     * @param name The name of the desired item.
     * 
     * @return the value of an item that previously added with putExtra() or null if no Bundle value
     * was found.
     * 
     * @see #putExtra(String, Bundle)
     */
    public Bundle getBundleExtra(String name) {
        return mExtras == null ? null : mExtras.getBundle(name);
    }

    /**
     * Retrieves a map of extended data from the intent.
     * 
     * @return the map of all extras previously added with putExtra(), or null if none have been
     * added.
     */
    public Bundle getExtras() {
        return (mExtras != null) ? new Bundle(mExtras) : null;
    }

    /**
     * Retrieve the concrete component associated with the intent. When receiving an intent, this is
     * the component that was found to best handle it (that is, yourself) and will always be
     * non-null; in all other cases it will be null unless explicitly set.
     * 
     * @return The name of the application component to handle the intent.
     * 
     * @see #setComponent
     */
    public ComponentName getComponent() {
        return mComponent;
    }

    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The boolean data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getBooleanExtra(String, boolean)
     */
    public Intent putExtra(String name, boolean value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putBoolean(name, value);
        return this;
    }

    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The byte data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getByteExtra(String, byte)
     */
    public Intent putExtra(String name, byte value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putByte(name, value);
        return this;
    }

    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The char data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getCharExtra(String, char)
     */
    public Intent putExtra(String name, char value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putChar(name, value);
        return this;
    }

    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The short data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getShortExtra(String, short)
     */
    public Intent putExtra(String name, short value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putShort(name, value);
        return this;
    }

    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The integer data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getIntExtra(String, int)
     */
    public Intent putExtra(String name, int value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putInt(name, value);
        return this;
    }

    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The long data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getLongExtra(String, long)
     */
    public Intent putExtra(String name, long value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putLong(name, value);
        return this;
    }

    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The float data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getFloatExtra(String, float)
     */
    public Intent putExtra(String name, float value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putFloat(name, value);
        return this;
    }

    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The double data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getDoubleExtra(String, double)
     */
    public Intent putExtra(String name, double value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putDouble(name, value);
        return this;
    }

    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The String data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getStringExtra(String)
     */
    public Intent putExtra(String name, String value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putString(name, value);
        return this;
    }

    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The boolean array data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getBooleanArrayExtra(String)
     */
    public Intent putExtra(String name, boolean[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putBooleanArray(name, value);
        return this;
    }

    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The byte array data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getByteArrayExtra(String)
     */
    public Intent putExtra(String name, byte[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putByteArray(name, value);
        return this;
    }

    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The short array data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getShortArrayExtra(String)
     */
    public Intent putExtra(String name, short[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putShortArray(name, value);
        return this;
    }

    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The char array data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getCharArrayExtra(String)
     */
    public Intent putExtra(String name, char[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putCharArray(name, value);
        return this;
    }

    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The int array data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getIntArrayExtra(String)
     */
    public Intent putExtra(String name, int[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putIntArray(name, value);
        return this;
    }

    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The byte array data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getLongArrayExtra(String)
     */
    public Intent putExtra(String name, long[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putLongArray(name, value);
        return this;
    }

    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The float array data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getFloatArrayExtra(String)
     */
    public Intent putExtra(String name, float[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putFloatArray(name, value);
        return this;
    }

    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The double array data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getDoubleArrayExtra(String)
     */
    public Intent putExtra(String name, double[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putDoubleArray(name, value);
        return this;
    }

    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The String array data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getStringArrayExtra(String)
     */
    public Intent putExtra(String name, String[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putStringArray(name, value);
        return this;
    }

    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The ArrayList<Integer> data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getIntegerArrayListExtra(String)
     */
    public Intent putIntegerArrayListExtra(String name, ArrayList value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putIntegerArrayList(name, value);
        return this;
    }

    /**
     * Add extended data to the intent.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The ArrayList<String> data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getStringArrayListExtra(String)
     */
    public Intent putStringArrayListExtra(String name, ArrayList value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putStringArrayList(name, value);
        return this;
    }

    /**
     * Add extended data to the intent. The name must include a package prefix, for example the app
     * com.android.contacts would use names like "com.android.contacts.ShowAll".
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The Bundle data value.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getBundleExtra(String)
     */
    public Intent putExtra(String name, Bundle value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putBundle(name, value);
        return this;
    }

    /**
     * Add a set of extended data to the intent.
     * 
     * @param extras The Bundle of extras to add to this intent.
     * 
     * @see #putExtra
     * @see #removeExtra
     */
    public Intent putExtras(Bundle extras) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putAll(extras);
        return this;
    }

    /**
     * Completely replace the extras in the Intent with the given Bundle of extras.
     * 
     * @param extras The new set of extras in the Intent, or null to erase all extras.
     */
    public Intent replaceExtras(Bundle extras) {
        mExtras = extras != null ? new Bundle(extras) : null;
        return this;
    }

    /**
     * Remove extended data from the intent.
     * 
     * @see #putExtra
     */
    public void removeExtra(String name) {
        if (mExtras != null) {
            mExtras.remove(name);
            if (mExtras.size() == 0) {
                mExtras = null;
            }
        }
    }

    /**
     * Explicitly set the component to handle the intent. If left with the default value of null,
     * the system will determine the appropriate class to use based on the other fields (action,
     * data, type, categories) in the Intent. If this class is defined, the specified class will
     * always be used regardless of the other fields. You should only set this value when you know
     * you absolutely want a specific class to be used; otherwise it is better to let the system
     * find the appropriate class so that you will respect the installed applications and user
     * preferences.
     * 
     * @param component The name of the application component to handle the intent, or null to let
     * the system find one for you.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #setClassName(String, String)
     * @see #getComponent
     */
    public Intent setComponent(ComponentName component) {
        mComponent = component;
        return this;
    }

    /**
     * Convenience for calling {@link #setComponent} with an explicit class name.
     * 
     * @param packageContext A Context of the application package implementing this class.
     * @param className The name of a class inside of the application package that will be used as
     * the component for this Intent.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #setComponent
     * @see #setClass
     */
    public Intent setClassName(Context packageContext, String className) {
        mComponent = new ComponentName(packageContext, className);
        return this;
    }

    /**
     * Convenience for calling {@link #setComponent} with an explicit application package name and
     * class name.
     * 
     * @param packageName The name of the package implementing the desired component.
     * @param className The name of a class inside of the application package that will be used as
     * the component for this Intent.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #setComponent
     */
    public Intent setClassName(String packageName, String className) {
        mComponent = new ComponentName(packageName, className);
        return this;
    }

    /**
     * Convenience for calling {@link #setComponent(ComponentName)} with the name returned by a
     * {@link Class} object.
     * 
     * @param packageContext A Context of the application package implementing this class.
     * @param cls The class name to set, equivalent to
     * <code>setClassName(context, cls.getName())</code>.
     * 
     * @return Returns the same Intent object, for chaining multiple calls into a single statement.
     * 
     * @see #setComponent
     */
    public Intent setClass(Context packageContext, Class cls) {
        mComponent = new ComponentName(packageContext, cls);
        return this;
    }
}
