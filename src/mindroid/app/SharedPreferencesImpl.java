/*
 * Copyright (C) 2010 The Android Open Source Project
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

import mindroid.content.Context;
import mindroid.content.SharedPreferences;
import mindroid.os.RemoteException;
import mindroid.util.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public final class SharedPreferencesImpl implements SharedPreferences {
    private static final String LOG_TAG = "SharedPreferences";
    private static final String UTF_8 = "UTF-8";
    private static final int SIZE_16_KB = 16 * 1024;
    private static final String MAP_TAG = "map";
    private static final String BOOLEAN_TAG = "boolean";
    private static final String INT_TAG = "int";
    private static final String LONG_TAG = "long";
    private static final String FLOAT_TAG = "float";
    private static final String STRING_TAG = "string";
    private static final String STRING_SET_TAG = "set";
    private static final String NAME_ATTR = "name";
    private static final String VALUE_ATTR = "value";

    private final File mFile;
    private final File mBackupFile;
    private final int mMode;
    private final Object mLock = new Object();
    private Map<String, Object> mMap;
    private Map<OnSharedPreferenceChangeListener, IOnSharedPreferenceChangeListener> mListeners = new HashMap<>();

    public SharedPreferencesImpl(File file, int mode) {
        mFile = file;
        mBackupFile = makeBackupFile(file);
        mMode = mode;
        synchronized (mLock) {
            mMap = null;
            loadSharedPrefs();
        }
    }

    @Override
    public Map<String, ?> getAll() {
        synchronized (mLock) {
            return new HashMap<>(mMap);
        }
    }

    @Override
    public String getString(String key, String defValue) {
        synchronized (mLock) {
            String v = (String) mMap.get(key);
            return v != null ? v : defValue;
        }
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        synchronized (mLock) {
            Set<String> v = (Set<String>) mMap.get(key);
            return v != null ? new HashSet<>(v) : defValues;
        }
    }

    @Override
    public int getInt(String key, int defValue) {
        synchronized (mLock) {
            Integer v = (Integer) mMap.get(key);
            return v != null ? v.intValue() : defValue;
        }
    }

    @Override
    public long getLong(String key, long defValue) {
        synchronized (mLock) {
            Long v = (Long) mMap.get(key);
            return v != null ? v.longValue() : defValue;
        }
    }

    @Override
    public float getFloat(String key, float defValue) {
        synchronized (mLock) {
            Float v = (Float) mMap.get(key);
            return v != null ? v.floatValue() : defValue;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        synchronized (mLock) {
            Boolean v = (Boolean) mMap.get(key);
            return v != null ? v.booleanValue() : defValue;
        }
    }

    @Override
    public boolean contains(String key) {
        synchronized (mLock) {
            return mMap.containsKey(key);
        }
    }

    @Override
    public Editor edit() {
        return new EditorImpl();
    }

    public final class EditorImpl implements Editor {
        private final Map<String, Object> mModifications = new HashMap<>();
        private boolean mClearMap = false;

        @Override
        public Editor putString(String key, String value) {
            synchronized (mLock) {
                mModifications.put(key, value);
                return this;
            }
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
            synchronized (mLock) {
                mModifications.put(key, (values == null) ? null : new HashSet<>(values));
                return this;
            }
        }

        @Override
        public Editor putInt(String key, int value) {
            synchronized (mLock) {
                mModifications.put(key, new Integer(value));
                return this;
            }
        }

        @Override
        public Editor putLong(String key, long value) {
            synchronized (mLock) {
                mModifications.put(key, new Long(value));
                return this;
            }
        }

        @Override
        public Editor putFloat(String key, float value) {
            synchronized (mLock) {
                mModifications.put(key, new Float(value));
                return this;
            }
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            synchronized (mLock) {
                mModifications.put(key, new Boolean(value));
                return this;
            }
        }

        @Override
        public Editor remove(String key) {
            synchronized (mLock) {
                mModifications.put(key, this);
                return this;
            }
        }

        @Override
        public Editor clear() {
            synchronized (mLock) {
                mClearMap = true;
                return this;
            }
        }

        @Override
        public void apply() {
            commit();
        }

        @Override
        public boolean commit() {
            synchronized (mLock) {
                boolean modifications = false;

                boolean hasListeners = !mListeners.isEmpty();
                List<String> modifiedKeys = null;
                if (hasListeners) {
                    modifiedKeys = new ArrayList<>();
                }

                if (mClearMap) {
                    if (!mMap.isEmpty()) {
                        modifications = true;
                        mMap.clear();
                    }
                    mClearMap = false;
                }

                Iterator<Map.Entry<String, Object>> itr = mModifications.entrySet().iterator();
                while (itr.hasNext()) {
                    Map.Entry<String, Object> entry = itr.next();
                    String k = entry.getKey();
                    Object v = entry.getValue();
                    // "this" is the magic value for entry removal.
                    // Setting a value to "null" is equivalent to removing the entry.
                    if (v == this || v == null) {
                        if (!mMap.containsKey(k)) {
                            continue;
                        }
                        mMap.remove(k);
                    } else {
                        if (mMap.containsKey(k)) {
                            Object existingValue = mMap.get(k);
                            if (existingValue != null && existingValue.equals(v)) {
                                continue;
                            }
                        }
                        mMap.put(k, v);
                    }

                    modifications = true;
                    if (hasListeners) {
                        modifiedKeys.add(k);
                    }
                }

                mModifications.clear();

                boolean result = false;
                if (modifications) {
                    result = storeSharedPrefs();
                    if (result && hasListeners) {
                        notifySharedPreferenceChangeListeners(modifiedKeys);
                    }
                }

                return result;
            }
        }
    }

    private void loadSharedPrefs() {
        if (mMap != null) {
            return;
        }
        if (mBackupFile.exists()) {
            Log.d(LOG_TAG, "Backup file " + mBackupFile + " found, restoring to " + mFile);
            if (!mFile.delete()) {
                Log.e(LOG_TAG, "Cannot delete file " + mFile + " to restore backup file " + mBackupFile);
            }
            if (!mBackupFile.renameTo(mFile)) {
                Log.e(LOG_TAG, "Cannot restore backup file " + mBackupFile + " to " + mFile);
            }
        }

        Map<String, Object> map = null;
        try {
            if (mFile.canRead()) {
                BufferedInputStream is = null;
                try {
                    is = new BufferedInputStream(new FileInputStream(mFile), SIZE_16_KB);
                    map = readMap(is);
                } catch (XmlPullParserException e) {
                    Log.w(LOG_TAG, "Cannot read file: " + mFile.getName(), e);
                } catch (FileNotFoundException e) {
                    Log.w(LOG_TAG, "Cannot read file: " + mFile.getName(), e);
                } catch (IOException e) {
                    Log.w(LOG_TAG, "Cannot read file: " + mFile.getName(), e);
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Cannot read file: " + mFile.getName());
        }
        if (map != null) {
            mMap = map;
        } else {
            mMap = new HashMap<>();
        }
    }

    private boolean storeSharedPrefs() {
        if (mFile.exists()) {
            if (!mBackupFile.exists()) {
                if (!mFile.renameTo(mBackupFile)) {
                    Log.e(LOG_TAG, "Cannot rename file " + mFile + " to backup file " + mBackupFile);
                    return false;
                }
            } else {
                if (!mFile.delete()) {
                    Log.e(LOG_TAG, "Cannot clean up file: " + mFile);
                }
            }
        }

        try {
            writeMap(mFile, mMap);
            fsync(mFile);
            if (mBackupFile.exists()) {
                if (!mBackupFile.delete()) {
                    Log.e(LOG_TAG, "Cannot clean up backup file " + mBackupFile);
                }
            }
            fsync(mFile.getParentFile());
            return true;
        } catch (XmlPullParserException e) {
            Log.w(LOG_TAG, "Cannot write file: " + mFile.getName(), e);
        } catch (IOException e) {
            Log.w(LOG_TAG, "Cannot write file: " + mFile.getName(), e);
        }

        // Clean up an unsuccessfully written file.
        if (mFile.exists()) {
            if (!mFile.delete()) {
                Log.e(LOG_TAG, "Cannot clean up partially-written file " + mFile);
            }
        }

        fsync(mFile.getParentFile());
        return false;
    }

    private class OnSharedPreferenceChangeListenerWrapper extends IOnSharedPreferenceChangeListener.Stub {
        private OnSharedPreferenceChangeListener mListener;

        OnSharedPreferenceChangeListenerWrapper(OnSharedPreferenceChangeListener listener) {
            mListener = listener;
        }

        @Override
        public void onSharedPreferenceChanged(String key) throws RemoteException {
            mListener.onSharedPreferenceChanged(SharedPreferencesImpl.this, key);
        }

        @Override
        public void onSharedPreferenceChanged() throws RemoteException {
            mListener.onSharedPreferenceChanged(SharedPreferencesImpl.this);
        }
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        if (listener != null) {
            synchronized (mLock) {
                if (!mListeners.containsKey(listener)) {
                    OnSharedPreferenceChangeListenerWrapper wrapper = new OnSharedPreferenceChangeListenerWrapper(listener);
                    mListeners.put(listener, IOnSharedPreferenceChangeListener.Stub.asInterface(wrapper.asBinder()));
                }
            }
        }
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        if (listener != null) {
            synchronized (mLock) {
                mListeners.remove(listener);
            }
        }
    }

    private static File makeBackupFile(File prefsFile) {
        return new File(prefsFile.getPath() + ".bak");
    }

    private void notifySharedPreferenceChangeListeners(final List<String> keys) {
        for (int i = 0; i < keys.size(); i++) {
            String key = (String) keys.get(i);
            Iterator<Map.Entry<OnSharedPreferenceChangeListener, IOnSharedPreferenceChangeListener>> itr = mListeners.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<OnSharedPreferenceChangeListener, IOnSharedPreferenceChangeListener> entry = itr.next();
                IOnSharedPreferenceChangeListener listener = entry.getValue();
                try {
                    listener.onSharedPreferenceChanged(key);
                } catch (RemoteException e) {
                    itr.remove();
                }
            }
        }

        Iterator<Map.Entry<OnSharedPreferenceChangeListener, IOnSharedPreferenceChangeListener>> itr = mListeners.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<OnSharedPreferenceChangeListener, IOnSharedPreferenceChangeListener> entry = itr.next();
            IOnSharedPreferenceChangeListener listener = (IOnSharedPreferenceChangeListener) entry.getValue();
            try {
                listener.onSharedPreferenceChanged();
            } catch (RemoteException e) {
                itr.remove();
            }
        }
    }

    private Map<String, Object> readMap(InputStream is) throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(is, UTF_8);
        parser.require(XmlPullParser.START_DOCUMENT, null, null);
        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, MAP_TAG);

        Map<String, Object> map = new HashMap<>();
        for (int eventType = parser.nextTag(); !parser.getName().equals(MAP_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
            if (parser.getName().equals(BOOLEAN_TAG)) {
                parseBoolean(parser, map);
            } else if (parser.getName().equals(INT_TAG)) {
                parseInt(parser, map);
            } else if (parser.getName().equals(LONG_TAG)) {
                parseLong(parser, map);
            } else if (parser.getName().equals(FLOAT_TAG)) {
                parseFloat(parser, map);
            } else if (parser.getName().equals(STRING_TAG)) {
                parseString(parser, map);
            } else if (parser.getName().equals(STRING_SET_TAG)) {
                parseStringSet(parser, map);
            } else {
                String tag = parser.getName();
                skipSubTree(parser);
                parser.require(XmlPullParser.END_TAG, null, tag);
            }
        }

        parser.require(XmlPullParser.END_TAG, null, MAP_TAG);
        parser.next();
        parser.require(XmlPullParser.END_DOCUMENT, null, null);

        return map;
    }

    private static void parseBoolean(XmlPullParser parser, Map<String, Object> map) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, BOOLEAN_TAG);

        String name = null;
        Boolean value = null;
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            String attributeValue = parser.getAttributeValue(i);
            if (attributeName.equals("name")) {
                name = attributeValue;
            } else if (attributeName.equals("value")) {
                if (attributeValue.equals("true")) {
                    value = Boolean.valueOf(true);
                } else {
                    value = Boolean.valueOf(false);
                }
            }
        }
        parser.nextTag();

        parser.require(XmlPullParser.END_TAG, null, BOOLEAN_TAG);

        if ((name != null) && (value != null)) {
            map.put(name, value);
        }
    }

    private static void parseInt(XmlPullParser parser, Map<String, Object> map) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, INT_TAG);

        String name = null;
        Integer value = null;
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            String attributeValue = parser.getAttributeValue(i);
            if (attributeName.equals("name")) {
                name = attributeValue;
            } else if (attributeName.equals("value")) {
                try {
                    value = new Integer(attributeValue);
                } catch (NumberFormatException e) {
                }
            }
        }
        parser.nextTag();

        parser.require(XmlPullParser.END_TAG, null, INT_TAG);

        if ((name != null) && (value != null)) {
            map.put(name, value);
        }
    }

    private static void parseLong(XmlPullParser parser, Map<String, Object> map) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, LONG_TAG);

        String name = null;
        Long value = null;
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            String attributeValue = parser.getAttributeValue(i);
            if (attributeName.equals("name")) {
                name = attributeValue;
            } else if (attributeName.equals("value")) {
                try {
                    value = new Long(attributeValue);
                } catch (NumberFormatException e) {
                }
            }
        }
        parser.nextTag();

        parser.require(XmlPullParser.END_TAG, null, LONG_TAG);

        if ((name != null) && (value != null)) {
            map.put(name, value);
        }
    }

    private static void parseFloat(XmlPullParser parser, Map<String, Object> map) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, FLOAT_TAG);

        String name = null;
        Float value = null;
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            String attributeValue = parser.getAttributeValue(i);
            if (attributeName.equals(NAME_ATTR)) {
                name = attributeValue;
            } else if (attributeName.equals(VALUE_ATTR)) {
                try {
                    value = new Float(attributeValue);
                } catch (NumberFormatException e) {
                }
            }
        }
        parser.nextTag();

        parser.require(XmlPullParser.END_TAG, null, FLOAT_TAG);

        if ((name != null) && (value != null)) {
            map.put(name, value);
        }
    }

    private static void parseString(XmlPullParser parser, Map<String, Object> map) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, STRING_TAG);

        String name = null;
        String value = null;
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            String attributeValue = parser.getAttributeValue(i);
            if (attributeName.equals("name")) {
                name = attributeValue;
            }
        }
        value = parser.nextText();

        parser.require(XmlPullParser.END_TAG, null, STRING_TAG);

        if (name != null) {
            map.put(name, (value != null) ? value : "");
        }
    }

    private static void parseStringSet(XmlPullParser parser, Map<String, Object> map) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, STRING_SET_TAG);

        String name = null;
        Set<String> value = null;
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            String attributeValue = parser.getAttributeValue(i);
            if (attributeName.equals("name")) {
                name = attributeValue;
            }
        }

        value = new HashSet<>();
        for (int eventType = parser.nextTag(); !parser.getName().equals(STRING_SET_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
            if (parser.getName().equals(STRING_TAG)) {
                parser.require(XmlPullParser.START_TAG, null, STRING_TAG);
                value.add(parser.nextText());
                parser.require(XmlPullParser.END_TAG, null, STRING_TAG);
            } else {
                String tag = parser.getName();
                skipSubTree(parser);
                parser.require(XmlPullParser.END_TAG, null, tag);
            }
        }

        parser.require(XmlPullParser.END_TAG, null, STRING_SET_TAG);

        if (name != null) {
            map.put(name, value);
        }
    }

    private void writeMap(File file, Map<String, Object> map) throws XmlPullParserException, IOException {
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(file);
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlSerializer serializer = factory.newSerializer();
            serializer.setOutput(os, UTF_8);
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startDocument(UTF_8, true);
            serializer.startTag(null, MAP_TAG);
            Iterator<Map.Entry<String, Object>> itr = map.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, Object> entry = itr.next();
                writeValue(serializer, entry.getKey(), entry.getValue());
            }
            serializer.endTag(null, MAP_TAG);
            serializer.endDocument();
        } catch (FileNotFoundException e) {
            throw e;
        } catch (XmlPullParserException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } finally {
            if (os != null) {
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static final void writeValue(XmlSerializer serializer, String name, Object value) throws XmlPullParserException, java.io.IOException {
        if (name == null || value == null) {
            return;
        } else if (value instanceof Boolean) {
            serializer.startTag(null, BOOLEAN_TAG);
            serializer.attribute(null, NAME_ATTR, name);
            serializer.attribute(null, VALUE_ATTR, value.toString());
            serializer.endTag(null, BOOLEAN_TAG);
        } else if (value instanceof Integer) {
            serializer.startTag(null, INT_TAG);
            serializer.attribute(null, NAME_ATTR, name);
            serializer.attribute(null, VALUE_ATTR, value.toString());
            serializer.endTag(null, INT_TAG);
        } else if (value instanceof Long) {
            serializer.startTag(null, LONG_TAG);
            serializer.attribute(null, NAME_ATTR, name);
            serializer.attribute(null, VALUE_ATTR, value.toString());
            serializer.endTag(null, LONG_TAG);
        } else if (value instanceof Float) {
            serializer.startTag(null, FLOAT_TAG);
            serializer.attribute(null, NAME_ATTR, name);
            serializer.attribute(null, VALUE_ATTR, value.toString());
            serializer.endTag(null, FLOAT_TAG);
        } else if (value instanceof String) {
            serializer.startTag(null, STRING_TAG);
            serializer.attribute(null, NAME_ATTR, name);
            serializer.text(value.toString());
            serializer.endTag(null, STRING_TAG);
        } else if (value instanceof Set) {
            serializer.startTag(null, STRING_SET_TAG);
            serializer.attribute(null, NAME_ATTR, name);
            Iterator<String> itr = ((Set<String>) value).iterator();
            while (itr.hasNext()) {
                serializer.startTag(null, STRING_TAG);
                serializer.text(itr.next().toString());
                serializer.endTag(null, STRING_TAG);
            }
            serializer.endTag(null, STRING_SET_TAG);
        } else {
            throw new RuntimeException("SharedPreferences.writeValue: Unable to write value " + value);
        }
    }

    private static void skipSubTree(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, null);
        int level = 1;
        while (level > 0) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.END_TAG) {
                --level;
            } else if (eventType == XmlPullParser.START_TAG) {
                ++level;
            }
        }
    }

    private boolean fsync(File file) {
        if (file == null) {
            return false;
        }
        if ((mMode & Context.MODE_NO_SYNC) != 0) {
            return true;
        }

        int attempts = 2;
        boolean synced = false;
        while (attempts > 0 && !synced) {
            boolean interrupted = Thread.interrupted();
            try (final FileChannel fc = FileChannel.open(file.toPath(), file.isDirectory() ? StandardOpenOption.READ : StandardOpenOption.WRITE)) {
                fc.force(true);
                synced = true;
            } catch (IOException e) {
                Log.w(LOG_TAG, "Cannot sync " + (file.isDirectory() ? "directory " : "file ") + file, e);
                synced = false;
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
                attempts--;
            }
        }
        return synced;
    }
}
