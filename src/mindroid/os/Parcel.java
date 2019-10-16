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

package mindroid.os;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import mindroid.io.ByteArrayInputStream;
import mindroid.io.ByteArrayOutputStream;
import mindroid.io.DataInputStream;
import mindroid.io.DataOutputStream;
import mindroid.runtime.system.Runtime;

public final class Parcel {
    private ByteArrayOutputStream mOutputStream;
    private ByteArrayInputStream mInputStream;
    private DataOutputStream mDataOutputStream;
    private DataInputStream mDataInputStream;
    private Bundle mExtras;

    private Parcel() {
        mOutputStream = new ByteArrayOutputStream();
        mDataOutputStream = new DataOutputStream(mOutputStream);
    }

    private Parcel(int size) {
        mOutputStream = new ByteArrayOutputStream(size);
        mDataOutputStream = new DataOutputStream(mOutputStream);
    }

    private Parcel(byte[] buffer, int offset, int size) {
        if (offset == 0 && size == buffer.length) {
            mOutputStream = new ByteArrayOutputStream(buffer);
        } else {
            mOutputStream = new ByteArrayOutputStream(size);
            mOutputStream.write(buffer, offset, size);
        }
        mDataOutputStream = new DataOutputStream(mOutputStream);
        asInput();
    }

    /**
     * Retrieve a new Parcel object from the pool.
     */
    public static Parcel obtain() {
        return new Parcel();
    }

    public static Parcel obtain(int size) {
        return new Parcel(size);
    }

    public static Parcel obtain(byte[] buffer) {
        if (buffer == null) {
            throw new NullPointerException();
        }
        return new Parcel(buffer, 0, buffer.length);
    }

    public static Parcel obtain(byte[] buffer, int offset, int size) {
        if (buffer == null) {
            throw new NullPointerException();
        }
        return new Parcel(buffer, offset, size);
    }

    /**
     * Put a Parcel object back into the pool.  You must not touch
     * the object after this call.
     */
    public final void recycle() {
    }

    /**
     * Returns the total amount of data contained in the parcel.
     */
    public final int size() {
        return mOutputStream.size();
    }

    /**
     * Write a boolean value into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public final void putBoolean(boolean value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeBoolean(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Write a byte value into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public final void putByte(byte value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeByte(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Write a character value into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public final void putChar(char value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeChar(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Write a short integer value into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public final void putShort(short value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeShort(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Write an integer value into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public final void putInt(int value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeInt(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Write a long integer value into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public final void putLong(long value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeLong(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Write a floating point value into the parcel at the current
     * dataPosition(), growing dataCapacity() if needed.
     */
    public final void putFloat(float value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeFloat(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Write a double precision floating point value into the parcel at the
     * current dataPosition(), growing dataCapacity() if needed.
     */
    public final void putDouble(double value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeDouble(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Write a string value into the parcel at the current dataPosition(),
     * growing dataCapacity() if needed.
     */
    public final void putString(String value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeUTF(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    public final void putBytes(byte[] buffer) throws RemoteException {
        checkOutput();
        mOutputStream.write(buffer);
    }

    public final void putBytes(byte[] buffer, int offset, int size) throws RemoteException {
        checkOutput();
        mOutputStream.write(buffer, offset, size);
    }

    public final void putBinder(IBinder binder) throws RemoteException {
        try {
            URI descriptor = new URI(binder.getInterfaceDescriptor());
            URI uri = new URI(binder.getUri().getScheme(), binder.getUri().getAuthority(), "/if=" + descriptor.getPath().substring(1), descriptor.getQuery(), null);
            putString(uri.toString());
        } catch (URISyntaxException e) {
            throw new RemoteException(e);
        }
    }

    public final void putBinder(IBinder base, IBinder binder) throws RemoteException {
        try {
            URI descriptor = new URI(binder.getInterfaceDescriptor());
            URI uri = new URI(base.getUri().getScheme(), binder.getUri().getAuthority(), "/if=" + descriptor.getPath().substring(1), descriptor.getQuery(), null);
            putString(uri.toString());
        } catch (URISyntaxException e) {
            throw new RemoteException(e);
        }
    }

    public final boolean getBoolean() throws RemoteException {
        checkInput();
        try {
            return mDataInputStream.readBoolean();
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    public final byte getByte() throws RemoteException {
        checkInput();
        try {
            return mDataInputStream.readByte();
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    public final char getChar() throws RemoteException {
        checkInput();
        try {
            return mDataInputStream.readChar();
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    public final short getShort() throws RemoteException {
        checkInput();
        try {
            return mDataInputStream.readShort();
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Read an integer value from the parcel at the current dataPosition().
     */
    public final int getInt() throws RemoteException {
        checkInput();
        try {
            return mDataInputStream.readInt();
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Read a long integer value from the parcel at the current dataPosition().
     */
    public final long getLong() throws RemoteException {
        checkInput();
        try {
            return mDataInputStream.readLong();
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Read a floating point value from the parcel at the current
     * dataPosition().
     * @throws IOException 
     */
    public final float getFloat() throws RemoteException {
        checkInput();
        try {
            return mDataInputStream.readFloat();
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Read a double precision floating point value from the parcel at the
     * current dataPosition().
     * @throws IOException 
     */
    public final double getDouble() throws RemoteException {
        checkInput();
        try {
            return mDataInputStream.readDouble();
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    /**
     * Read a string value from the parcel at the current dataPosition().
     * @throws IOException 
     */
    public final String getString() throws RemoteException {
        checkInput();
        try {
            return mDataInputStream.readUTF();
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    public final byte[] getBytes() throws RemoteException {
        checkInput();
        if (mInputStream.available() >= 0) {
            byte[] buffer = new byte[mInputStream.available()];
            mInputStream.read(buffer);
            return buffer;
        } else {
            return null;
        }
    }

    public final byte[] getBytes(int size) throws RemoteException {
        checkInput();
        if (mInputStream.available() >= 0) {
            size = Math.min(mInputStream.available(), size);
            byte[] buffer = new byte[size];
            mInputStream.read(buffer, 0, size);
            return buffer;
        } else {
            return null;
        }
    }

    public final IBinder getBinder() throws RemoteException {
        URI uri;
        try {
            uri = new URI(getString());
        } catch (URISyntaxException e) {
            return null;
        }
        return Runtime.getRuntime().getBinder(uri);
    }

    public final byte[] getByteArray() {
        return mOutputStream.getByteArray();
    }

    public final ByteArrayInputStream asInputStream() {
        if (mInputStream == null) {
            mInputStream = new ByteArrayInputStream(mOutputStream.getByteArray(), 0, mOutputStream.size());
            mDataInputStream = new DataInputStream(mInputStream);
        }
        return mInputStream;
    }

    public final ByteArrayOutputStream asOutputStream() {
        if (mInputStream != null) {
            mInputStream = null;
            mDataInputStream = null;
        }
        return mOutputStream;
    }

    public final Parcel asInput() {
        asInputStream();
        return this;
    }

    public final Parcel asOutput() {
        asOutputStream();
        return this;
    }

    private final void checkOutput() {
        if (mInputStream != null) {
            throw new IllegalStateException("Parcel is in input mode");
        }
    }

    private final void checkInput() {
        if (mInputStream == null) {
            throw new IllegalStateException("Parcel is in output mode");
        }
    }

    public static final URI toUri(IBinder base, IBinder binder) throws RemoteException {
        try {
            URI descriptor = new URI(binder.getInterfaceDescriptor());
            URI uri = new URI(base.getUri().getScheme(), binder.getUri().getAuthority(), "/if=" + descriptor.getPath().substring(1), descriptor.getQuery(), null);
            return uri;
        } catch (URISyntaxException e) {
            throw new RemoteException(e);
        }
    }

    public static final IBinder fromUri(URI uri) throws RemoteException {
        return Runtime.getRuntime().getBinder(uri);
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
     * Retrieve extended data from the parcel.
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
     * Retrieve extended data from the parcel.
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
     * Retrieve extended data from the parcel.
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
     * Retrieve extended data from the parcel.
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
     * Retrieve extended data from the parcel.
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
     * Retrieve extended data from the parcel.
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
     * Retrieve extended data from the parcel.
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
     * Retrieve extended data from the parcel.
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
     * Retrieve extended data from the parcel.
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
     * Retrieve extended data from the parcel.
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
     * Retrieve extended data from the parcel.
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
     * Retrieve extended data from the parcel.
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
     * Retrieve extended data from the parcel.
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
     * Retrieve extended data from the parcel.
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
     * Retrieve extended data from the parcel.
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
     * Retrieve extended data from the parcel.
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
     * Retrieve extended data from the parcel.
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
     * Retrieve extended data from the parcel.
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
     * Retrieve extended data from the parcel.
     * 
     * @param name The name of the desired item.
     * 
     * @return the value of an item that previously added with putExtra() or null if no
     * ArrayList<Integer> value was found.
     * 
     * @see #putIntegerArrayListExtra(String, ArrayList)
     */
    public ArrayList<Integer> getIntegerArrayListExtra(String name) {
        return mExtras == null ? null : mExtras.getIntegerArrayList(name);
    }

    /**
     * Retrieve extended data from the parcel.
     * 
     * @param name The name of the desired item.
     * 
     * @return the value of an item that previously added with putExtra() or null if no
     * ArrayList<String> value was found.
     * 
     * @see #putStringArrayListExtra(String, ArrayList)
     */
    public ArrayList<String> getStringArrayListExtra(String name) {
        return mExtras == null ? null : mExtras.getStringArrayList(name);
    }

    /**
     * Retrieve extended data from the parcel.
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
     * Retrieves a map of extended data from the parcel.
     * 
     * @return the map of all extras previously added with putExtra(), or null if none have been
     * added.
     */
    public Bundle getExtras() {
        return (mExtras != null) ? new Bundle(mExtras) : null;
    }
    
    /**
     * Add extended data to the parcel.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The boolean data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getBooleanExtra(String, boolean)
     */
    public Parcel putExtra(String name, boolean value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putBoolean(name, value);
        return this;
    }

    /**
     * Add extended data to the parcel.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The byte data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getByteExtra(String, byte)
     */
    public Parcel putExtra(String name, byte value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putByte(name, value);
        return this;
    }

    /**
     * Add extended data to the parcel.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The char data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getCharExtra(String, char)
     */
    public Parcel putExtra(String name, char value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putChar(name, value);
        return this;
    }

    /**
     * Add extended data to the parcel.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The short data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getShortExtra(String, short)
     */
    public Parcel putExtra(String name, short value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putShort(name, value);
        return this;
    }

    /**
     * Add extended data to the parcel.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The integer data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getIntExtra(String, int)
     */
    public Parcel putExtra(String name, int value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putInt(name, value);
        return this;
    }

    /**
     * Add extended data to the parcel.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The long data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getLongExtra(String, long)
     */
    public Parcel putExtra(String name, long value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putLong(name, value);
        return this;
    }

    /**
     * Add extended data to the parcel.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The float data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getFloatExtra(String, float)
     */
    public Parcel putExtra(String name, float value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putFloat(name, value);
        return this;
    }

    /**
     * Add extended data to the parcel.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The double data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getDoubleExtra(String, double)
     */
    public Parcel putExtra(String name, double value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putDouble(name, value);
        return this;
    }

    /**
     * Add extended data to the parcel.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The String data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getStringExtra(String)
     */
    public Parcel putExtra(String name, String value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putString(name, value);
        return this;
    }

    /**
     * Add extended data to the parcel.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The boolean array data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getBooleanArrayExtra(String)
     */
    public Parcel putExtra(String name, boolean[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putBooleanArray(name, value);
        return this;
    }

    /**
     * Add extended data to the parcel.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The byte array data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getByteArrayExtra(String)
     */
    public Parcel putExtra(String name, byte[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putByteArray(name, value);
        return this;
    }

    /**
     * Add extended data to the parcel.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The short array data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getShortArrayExtra(String)
     */
    public Parcel putExtra(String name, short[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putShortArray(name, value);
        return this;
    }

    /**
     * Add extended data to the parcel.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The char array data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getCharArrayExtra(String)
     */
    public Parcel putExtra(String name, char[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putCharArray(name, value);
        return this;
    }

    /**
     * Add extended data to the parcel.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The int array data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getIntArrayExtra(String)
     */
    public Parcel putExtra(String name, int[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putIntArray(name, value);
        return this;
    }

    /**
     * Add extended data to the parcel.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The byte array data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getLongArrayExtra(String)
     */
    public Parcel putExtra(String name, long[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putLongArray(name, value);
        return this;
    }

    /**
     * Add extended data to the parcel.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The float array data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getFloatArrayExtra(String)
     */
    public Parcel putExtra(String name, float[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putFloatArray(name, value);
        return this;
    }

    /**
     * Add extended data to the parcel.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The double array data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getDoubleArrayExtra(String)
     */
    public Parcel putExtra(String name, double[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putDoubleArray(name, value);
        return this;
    }

    /**
     * Add extended data to the parcel.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The String array data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getStringArrayExtra(String)
     */
    public Parcel putExtra(String name, String[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putStringArray(name, value);
        return this;
    }

    /**
     * Add extended data to the parcel.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The ArrayList<Integer> data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getIntegerArrayListExtra(String)
     */
    public Parcel putIntegerArrayListExtra(String name, ArrayList<Integer> value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putIntegerArrayList(name, value);
        return this;
    }

    /**
     * Add extended data to the parcel.
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The ArrayList<String> data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getStringArrayListExtra(String)
     */
    public Parcel putStringArrayListExtra(String name, ArrayList<String> value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putStringArrayList(name, value);
        return this;
    }

    /**
     * Add extended data to the parcel. The name must include a package prefix, for example the app
     * com.android.contacts would use names like "com.android.contacts.ShowAll".
     * 
     * @param name The name of the extra data, with package prefix.
     * @param value The Bundle data value.
     * 
     * @return Returns the same Parcel object, for chaining multiple calls into a single statement.
     * 
     * @see #putExtras
     * @see #removeExtra
     * @see #getBundleExtra(String)
     */
    public Parcel putExtra(String name, Bundle value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putBundle(name, value);
        return this;
    }

    /**
     * Add a set of extended data to the parcel.
     * 
     * @param extras The Bundle of extras to add to this parcel.
     * 
     * @see #putExtra
     * @see #removeExtra
     */
    public Parcel putExtras(Bundle extras) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putAll(extras);
        return this;
    }

    /**
     * Completely replace the extras in the Parcel with the given Bundle of extras.
     * 
     * @param extras The new set of extras in the Parcel, or null to erase all extras.
     */
    public Parcel replaceExtras(Bundle extras) {
        mExtras = extras != null ? new Bundle(extras) : null;
        return this;
    }

    /**
     * Remove extended data from the parcel.
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Parcel {data=[");
        byte[] data = mOutputStream.getByteArray();
        for (int i = 0; i < mOutputStream.size(); ++i) {
            builder.append(String.format("%02X", Byte.toUnsignedInt(data[i])));
        }
        builder.append("]");

        if (mExtras != null) {
            builder.append(", extras=");
            builder.append(mExtras.toString());
        }

        return builder.append("}").toString();
    }
}
