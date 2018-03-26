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

    public final void putBoolean(boolean value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeBoolean(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    public final void putByte(byte value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeByte(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

    public final void putChar(char value) throws RemoteException {
        checkOutput();
        try {
            mDataOutputStream.writeChar(value);
        } catch (IOException e) {
            throw new RemoteException(e);
        }
    }

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
            URI uri = new URI(binder.getUri().getScheme(), binder.getUri().getAuthority(), "/if=" + descriptor.getPath().substring(1), null, null);
            putString(uri.toString());
        } catch (URISyntaxException e) {
            throw new RemoteException(e);
        }
    }

    public final void putBinder(IBinder base, IBinder binder) throws RemoteException {
        try {
            URI descriptor = new URI(binder.getInterfaceDescriptor());
            URI uri = new URI(base.getUri().getScheme(), binder.getUri().getAuthority(), "/if=" + descriptor.getPath().substring(1), null, null);
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

    public final byte[] toByteArray() {
        return mOutputStream.getByteArray();
    }

    public final ByteArrayInputStream asInputStream() {
        if (mInputStream == null) {
            mInputStream = new ByteArrayInputStream(mOutputStream.getByteArray());
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
            URI uri = new URI(base.getUri().getScheme(), binder.getUri().getAuthority(), "/if=" + descriptor.getPath().substring(1), null, null);
            return uri;
        } catch (URISyntaxException e) {
            throw new RemoteException(e);
        }
    }

    public static final IBinder fromUri(URI uri) throws RemoteException {
        return Runtime.getRuntime().getBinder(uri);
    }
}
