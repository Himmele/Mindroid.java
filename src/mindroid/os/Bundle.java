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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A mapping from String values to various types.
 *
 */
public final class Bundle {
    private HashMap<String, Object> mMap;

    public Bundle() {
        mMap = new HashMap<>();
    }

    /**
     * Constructs a Bundle containing a copy of the mappings from the given Bundle.
     *
     * @param other a Bundle to be copied.
     */
    public Bundle(Bundle other) {
        mMap = new HashMap<>();
        Iterator<Map.Entry<String, Object>> itr = other.mMap.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, Object> entry = itr.next();
            String key = entry.getKey();
            Object value = entry.getValue();
            mMap.put(key, value);
        }
    }

    /**
     * Clones the current Bundle. The internal map is cloned, but the keys and values to which it
     * refers are copied by reference.
     */
    public Object clone() {
        return new Bundle(this);
    }

    /**
     * Removes all elements from the mapping of this Bundle.
     */
    public void clear() {
        mMap.clear();
    }

    /**
     * Returns the number of mappings contained in this Bundle.
     *
     * @return the number of mappings as an int.
     */
    public int size() {
        return mMap.size();
    }

    /**
     * Returns true if the mapping of this Bundle is empty, false otherwise.
     */
    public boolean isEmpty() {
        return mMap.isEmpty();
    }

    /**
     * Returns true if the given key is contained in the mapping of this Bundle.
     *
     * @param key a String key
     * @return true if the key is part of the mapping, false otherwise
     */
    public boolean containsKey(String key) {
        return mMap.containsKey(key);
    }

    /**
     * Returns a Set containing the Strings used as keys in this Bundle.
     *
     * @return a Set of String keys
     */
    public Set<String> keySet() {
        return mMap.keySet();
    }

    /**
     * Returns the entry with the given key as an object.
     *
     * @param key a String key
     * @return an Object, or null
     */
    public Object get(String key) {
        return mMap.get(key);
    }

    /**
     * Removes any entry with the given key from the mapping of this Bundle.
     *
     * @param key a String key
     */
    public void remove(String key) {
        mMap.remove(key);
    }

    /**
     * Inserts all key-value pairs from the given Bundle into this Bundle.
     *
     * @param bundle a Bundle
     */
    public void putAll(Bundle bundle) {
        if (bundle != null) {
            mMap.putAll(bundle.mMap);
        }
    }

    /**
     * Inserts a Boolean value into the mapping of this Bundle, replacing any existing value for the
     * given key. Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a Boolean, or null
     */
    public void putBoolean(String key, boolean value) {
        mMap.put(key, new Boolean(value));
    }

    /**
     * Inserts a byte value into the mapping of this Bundle, replacing any existing value for the
     * given key.
     *
     * @param key a String, or null
     * @param value a byte
     */
    public void putByte(String key, byte value) {
        mMap.put(key, new Byte(value));
    }

    /**
     * Inserts a char value into the mapping of this Bundle, replacing any existing value for the
     * given key.
     *
     * @param key a String, or null
     * @param value a char, or null
     */
    public void putChar(String key, char value) {
        mMap.put(key, new Character(value));
    }

    /**
     * Inserts a short value into the mapping of this Bundle, replacing any existing value for the
     * given key.
     *
     * @param key a String, or null
     * @param value a short
     */
    public void putShort(String key, short value) {
        mMap.put(key, new Short(value));
    }

    /**
     * Inserts an int value into the mapping of this Bundle, replacing any existing value for the
     * given key.
     *
     * @param key a String, or null
     * @param value an int, or null
     */
    public void putInt(String key, int value) {
        mMap.put(key, new Integer(value));
    }

    /**
     * Inserts a long value into the mapping of this Bundle, replacing any existing value for the
     * given key.
     *
     * @param key a String, or null
     * @param value a long
     */
    public void putLong(String key, long value) {
        mMap.put(key, new Long(value));
    }

    /**
     * Inserts a float value into the mapping of this Bundle, replacing any existing value for the
     * given key.
     *
     * @param key a String, or null
     * @param value a float
     */
    public void putFloat(String key, float value) {
        mMap.put(key, new Float(value));
    }

    /**
     * Inserts a double value into the mapping of this Bundle, replacing any existing value for the
     * given key.
     *
     * @param key a String, or null
     * @param value a double
     */
    public void putDouble(String key, double value) {
        mMap.put(key, new Double(value));
    }

    /**
     * Inserts a String value into the mapping of this Bundle, replacing any existing value for the
     * given key. Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a String, or null
     */
    public void putString(String key, String value) {
        if (value != null) {
            mMap.put(key, value);
        }
    }

    /**
     * Inserts an object value into the mapping of this Bundle, replacing any existing value for the
     * given key. Either key or value may be null.
     *
     * @param key a String, or null
     * @param value an object, or null
     */
    public void putObject(String key, Object value) {
        if (value != null) {
            mMap.put(key, value);
        }
    }

    /**
     * Inserts a boolean array value into the mapping of this Bundle, replacing any existing value
     * for the given key. Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a boolean array object, or null
     */
    public void putBooleanArray(String key, boolean[] value) {
        if (value != null) {
            mMap.put(key, value);
        }
    }

    /**
     * Inserts a byte array value into the mapping of this Bundle, replacing any existing value for
     * the given key. Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a byte array object, or null
     */
    public void putByteArray(String key, byte[] value) {
        if (value != null) {
            mMap.put(key, value);
        }
    }

    /**
     * Inserts a short array value into the mapping of this Bundle, replacing any existing value for
     * the given key. Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a short array object, or null
     */
    public void putShortArray(String key, short[] value) {
        if (value != null) {
            mMap.put(key, value);
        }
    }

    /**
     * Inserts a char array value into the mapping of this Bundle, replacing any existing value for
     * the given key. Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a char array object, or null
     */
    public void putCharArray(String key, char[] value) {
        if (value != null) {
            mMap.put(key, value);
        }
    }

    /**
     * Inserts an int array value into the mapping of this Bundle, replacing any existing value for
     * the given key. Either key or value may be null.
     *
     * @param key a String, or null
     * @param value an int array object, or null
     */
    public void putIntArray(String key, int[] value) {
        if (value != null) {
            mMap.put(key, value);
        }
    }

    /**
     * Inserts a long array value into the mapping of this Bundle, replacing any existing value for
     * the given key. Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a long array object, or null
     */
    public void putLongArray(String key, long[] value) {
        if (value != null) {
            mMap.put(key, value);
        }
    }

    /**
     * Inserts a float array value into the mapping of this Bundle, replacing any existing value for
     * the given key. Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a float array object, or null
     */
    public void putFloatArray(String key, float[] value) {
        if (value != null) {
            mMap.put(key, value);
        }
    }

    /**
     * Inserts a double array value into the mapping of this Bundle, replacing any existing value
     * for the given key. Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a double array object, or null
     */
    public void putDoubleArray(String key, double[] value) {
        if (value != null) {
            mMap.put(key, value);
        }
    }

    /**
     * Inserts a String array value into the mapping of this Bundle, replacing any existing value
     * for the given key. Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a String array object, or null
     */
    public void putStringArray(String key, String[] value) {
        if (value != null) {
            mMap.put(key, value);
        }
    }

    /**
     * Inserts an ArrayList<Integer> value into the mapping of this Bundle, replacing any existing
     * value for the given key. Either key or value may be null.
     *
     * @param key a String, or null
     * @param value an ArrayList<Integer> object, or null
     */
    public void putIntegerArrayList(String key, ArrayList<Integer> value) {
        if (value != null) {
            mMap.put(key, value);
        }
    }

    /**
     * Inserts an ArrayList<String> value into the mapping of this Bundle, replacing any existing
     * value for the given key. Either key or value may be null.
     *
     * @param key a String, or null
     * @param value an ArrayList<String> object, or null
     */
    public void putStringArrayList(String key, ArrayList<String> value) {
        if (value != null) {
            mMap.put(key, value);
        }
    }

    /**
     * Inserts a Bundle value into the mapping of this Bundle, replacing any existing value for the
     * given key. Either key or value may be null.
     *
     * @param key a String, or null
     * @param value a Bundle object, or null
     */
    public void putBundle(String key, Bundle value) {
        if (value != null) {
            mMap.put(key, value);
        }
    }

    /**
     * Inserts an {@link IBinder} value into the mapping of this Bundle, replacing any existing
     * value for the given key. Either key or value may be null.
     *
     * <p class="note">
     * You should be very careful when using this function. In many places where Bundles are used
     * (such as inside of Intent objects), the Bundle can live longer inside of another process than
     * the process that had originally created it. In that case, the IBinder you supply here will
     * become invalid when your process goes away, and no longer usable, even if a new process is
     * created for you later on.
     * </p>
     *
     * @param key a String, or null
     * @param value an IBinder object, or null
     */
    public void putBinder(String key, IBinder value) {
        if (value != null) {
            mMap.put(key, value);
        }
    }

    /**
     * Returns the value associated with the given key, or false if no mapping of the desired type
     * exists for the given key.
     *
     * @param key a String
     * @return a boolean value
     */
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    /**
     * Returns the value associated with the given key, or defaultValue if no mapping of the desired
     * type exists for the given key.
     *
     * @param key a String
     * @param defaultValue Value to return if key does not exist
     * @return a boolean value
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object o = mMap.get(key);
        if (o == null) {
            return defaultValue;
        }
        try {
            return ((Boolean) o).booleanValue();
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the value associated with the given key, or (byte) 0 if no mapping of the desired
     * type exists for the given key.
     *
     * @param key a String
     * @return a byte value
     */
    public byte getByte(String key) {
        return getByte(key, (byte) 0);
    }

    /**
     * Returns the value associated with the given key, or defaultValue if no mapping of the desired
     * type exists for the given key.
     *
     * @param key a String
     * @param defaultValue Value to return if key does not exist
     * @return a byte value
     */
    public byte getByte(String key, byte defaultValue) {
        Object o = mMap.get(key);
        if (o == null) {
            return defaultValue;
        }
        try {
            return ((Byte) o).byteValue();
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the value associated with the given key, or (char) 0 if no mapping of the desired type
     * exists for the given key.
     *
     * @param key a String
     * @return a char value
     */
    public char getChar(String key) {
        return getChar(key, (char) 0);
    }

    /**
     * Returns the value associated with the given key, or defaultValue if no mapping of the desired
     * type exists for the given key.
     *
     * @param key a String
     * @param defaultValue Value to return if key does not exist
     * @return a char value
     */
    public char getChar(String key, char defaultValue) {
        Object o = mMap.get(key);
        if (o == null) {
            return defaultValue;
        }
        try {
            return ((Character) o).charValue();
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the value associated with the given key, or (short) 0 if no mapping of the desired
     * type exists for the given key.
     *
     * @param key a String
     * @return a short value
     */
    public short getShort(String key) {
        return getShort(key, (short) 0);
    }

    /**
     * Returns the value associated with the given key, or defaultValue if no mapping of the desired
     * type exists for the given key.
     *
     * @param key a String
     * @param defaultValue Value to return if key does not exist
     * @return a short value
     */
    public short getShort(String key, short defaultValue) {
        Object o = mMap.get(key);
        if (o == null) {
            return defaultValue;
        }
        try {
            return ((Short) o).shortValue();
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the value associated with the given key, or 0 if no mapping of the desired type
     * exists for the given key.
     *
     * @param key a String
     * @return an int value
     */
    public int getInt(String key) {
        return getInt(key, 0);
    }

    /**
     * Returns the value associated with the given key, or defaultValue if no mapping of the desired
     * type exists for the given key.
     *
     * @param key a String
     * @param defaultValue Value to return if key does not exist
     * @return an int value
     */
    public int getInt(String key, int defaultValue) {
        Object o = mMap.get(key);
        if (o == null) {
            return defaultValue;
        }
        try {
            return ((Integer) o).intValue();
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the value associated with the given key, or 0L if no mapping of the desired type
     * exists for the given key.
     *
     * @param key a String
     * @return a long value
     */
    public long getLong(String key) {
        return getLong(key, 0L);
    }

    /**
     * Returns the value associated with the given key, or defaultValue if no mapping of the desired
     * type exists for the given key.
     *
     * @param key a String
     * @param defaultValue Value to return if key does not exist
     * @return a long value
     */
    public long getLong(String key, long defaultValue) {
        Object o = mMap.get(key);
        if (o == null) {
            return defaultValue;
        }
        try {
            return ((Long) o).longValue();
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the value associated with the given key, or 0.0f if no mapping of the desired type
     * exists for the given key.
     *
     * @param key a String
     * @return a float value
     */
    public float getFloat(String key) {
        return getFloat(key, 0.0f);
    }

    /**
     * Returns the value associated with the given key, or defaultValue if no mapping of the desired
     * type exists for the given key.
     *
     * @param key a String
     * @param defaultValue Value to return if key does not exist
     * @return a float value
     */
    public float getFloat(String key, float defaultValue) {
        Object o = mMap.get(key);
        if (o == null) {
            return defaultValue;
        }
        try {
            return ((Float) o).floatValue();
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the value associated with the given key, or 0.0 if no mapping of the desired type
     * exists for the given key.
     *
     * @param key a String
     * @return a double value
     */
    public double getDouble(String key) {
        return getDouble(key, 0.0);
    }

    /**
     * Returns the value associated with the given key, or defaultValue if no mapping of the desired
     * type exists for the given key.
     *
     * @param key a String
     * @param defaultValue Value to return if key does not exist
     * @return a double value
     */
    public double getDouble(String key, double defaultValue) {
        Object o = mMap.get(key);
        if (o == null) {
            return defaultValue;
        }
        try {
            return ((Double) o).doubleValue();
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of the desired type
     * exists for the given key or a null value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a String value, or null
     */
    public String getString(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (String) o;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or defaultValue if no mapping of the desired
     * type exists for the given key.
     *
     * @param key a String, or null
     * @param defaultValue Value to return if key does not exist
     * @return a String value, or null
     */
    public String getString(String key, String defaultValue) {
        Object o = mMap.get(key);
        if (o == null) {
            return defaultValue;
        }
        try {
            return (String) o;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of the desired type
     * exists for the given key or a null value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return an object value, or null
     */
    public Object getObject(String key) {
        Object o = mMap.get(key);
        return o;
    }

    /**
     * Returns the value associated with the given key, or defaultValue if no mapping of the desired
     * type exists for the given key.
     *
     * @param key a String, or null
     * @param defaultValue Value to return if key does not exist
     * @return an object value, or null
     */
    public Object getObject(String key, Object defaultValue) {
        Object o = mMap.get(key);
        if (o == null) {
            return defaultValue;
        }
        return o;
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of the desired type
     * exists for the given key or a null value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a boolean[] value, or null
     */
    public boolean[] getBooleanArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (boolean[]) o;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of the desired type
     * exists for the given key or a null value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a byte[] value, or null
     */
    public byte[] getByteArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (byte[]) o;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of the desired type
     * exists for the given key or a null value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a short[] value, or null
     */
    public short[] getShortArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (short[]) o;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of the desired type
     * exists for the given key or a null value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a char[] value, or null
     */
    public char[] getCharArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (char[]) o;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of the desired type
     * exists for the given key or a null value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return an int[] value, or null
     */
    public int[] getIntArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (int[]) o;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of the desired type
     * exists for the given key or a null value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a long[] value, or null
     */
    public long[] getLongArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (long[]) o;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of the desired type
     * exists for the given key or a null value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a float[] value, or null
     */
    public float[] getFloatArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (float[]) o;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of the desired type
     * exists for the given key or a null value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a double[] value, or null
     */
    public double[] getDoubleArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (double[]) o;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of the desired type
     * exists for the given key or a null value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a String[] value, or null
     */
    public String[] getStringArray(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (String[]) o;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of the desired type
     * exists for the given key or a null value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return an ArrayList<String> value, or null
     */
    public ArrayList<Integer> getIntegerArrayList(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (ArrayList<Integer>) o;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of the desired type
     * exists for the given key or a null value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return an ArrayList<String> value, or null
     */
    public ArrayList<String> getStringArrayList(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (ArrayList<String>) o;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of the desired type
     * exists for the given key or a null value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return a Bundle value, or null
     */
    public Bundle getBundle(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (Bundle) o;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Returns the value associated with the given key, or null if no mapping of the desired type
     * exists for the given key or a null value is explicitly associated with the key.
     *
     * @param key a String, or null
     * @return an IBinder value, or null
     */
    public IBinder getBinder(String key) {
        Object o = mMap.get(key);
        if (o == null) {
            return null;
        }
        try {
            return (IBinder) o;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Retrieve all values from the Bundle.
     *
     * <p>
     * Note that you <em>must not</em> modify the collection returned by this method, or alter any
     * of its contents. The consistency of your stored data is not guaranteed if you do.
     *
     * @return Returns a map containing a list of pairs key/value representing the Bundle.
     *
     * @throws NullPointerException
     */
    public Map<String, ?> getAll() {
        return new HashMap<>(mMap);
    }

    /** @hide */
    public static boolean isBasicType(Object value) {
        return (value instanceof Boolean) || (value instanceof Byte)
                || (value instanceof Character) || (value instanceof Short)
                || (value instanceof Integer) || (value instanceof Long)
                || (value instanceof Float) || (value instanceof Double)
                || (value instanceof String) || (value instanceof boolean[])
                || (value instanceof byte[]) || (value instanceof char[])
                || (value instanceof short[]) || (value instanceof int[])
                || (value instanceof long[]) || (value instanceof float[])
                || (value instanceof double[]) || (value instanceof String[])
                || (value == null);
    }

    /**
     * Retain only basic types within the Bundle.
     *
     * @hide
     */
    public void retainBasicTypes() {
        if (mMap != null) {
            Iterator<Map.Entry<String, Object>> itr = mMap.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, Object> entry = itr.next();
                Object value = entry.getValue();

                if (isBasicType(value)) {
                    continue;
                }
                if (value instanceof Bundle) {
                    ((Bundle) value).retainBasicTypes();
                    continue;
                }
                itr.remove();
            }
        }
    }
}
