/*
 * Copyright (C) 2018 Daniel Himmelein
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

package examples.eliza.xmlrpc;

import mindroid.os.Parcel;
import mindroid.os.IBinder;
import mindroid.os.Binder;
import mindroid.os.RemoteException;
import mindroid.util.concurrent.Promise;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public interface IElizaListener extends examples.eliza.IElizaListener {
    public static class Stub extends Binder {
        private static final String DESCRIPTOR = "xmlrpc://interfaces/examples/eliza/IElizaListener";
        private final examples.eliza.IElizaListener mElizaListener;

        public Stub(Binder binder) {
            super(binder);
            mElizaListener = (examples.eliza.IElizaListener) binder.queryLocalInterface(binder.getInterfaceDescriptor());
            this.attachInterface(mElizaListener, DESCRIPTOR);
        }

        @Override
        protected void onTransact(int what, Parcel data, Promise<Parcel> result) throws RemoteException {
            switch (what) {
            case MSG_ON_REPLY: {
                String reply;
                try {
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    factory.setNamespaceAware(true);
                    XmlPullParser parser = factory.newPullParser();
                    parser.setInput(data.asInputStream(), "UTF-8");
                    parser.require(XmlPullParser.START_DOCUMENT, null, null);
                    parser.nextTag();
                    parser.require(XmlPullParser.START_TAG, null, "params");
                    parser.nextTag();
                    parser.require(XmlPullParser.START_TAG, null, "param");
                    parser.nextTag();
                    parser.require(XmlPullParser.START_TAG, null, "value");
                    parser.nextTag();
                    parser.require(XmlPullParser.START_TAG, null, "string");
                    reply = parser.nextText();
                    parser.require(XmlPullParser.END_TAG, null, "string");
                    parser.nextTag();
                    parser.require(XmlPullParser.END_TAG, null, "value");
                    parser.nextTag();
                    parser.require(XmlPullParser.END_TAG, null, "param");
                    parser.nextTag();
                    parser.require(XmlPullParser.END_TAG, null, "params");
                    parser.next();
                    parser.require(XmlPullParser.END_DOCUMENT, null, null);
                } catch (Exception e) {
                    throw new RemoteException("Binder transaction failure");
                }
                mElizaListener.onReply(reply);
                break;
            }
            default:
                super.onTransact(what, data, result);
                break;
            }
        }

        public static class Proxy implements IElizaListener {
            private final IBinder mRemote;

            public Proxy(IBinder remote) {
                mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return mRemote;
            }

            @Override
            public boolean equals(final Object obj) {
                if (obj == null) return false;
                if (obj == this) return true;
                if (obj instanceof Stub.Proxy) {
                    final Stub.Proxy that = (Stub.Proxy) obj;
                    return this.mRemote.equals(that.mRemote);
                }
                return false;
            }

            @Override
            public int hashCode() {
                return mRemote.hashCode();
            }

            @Override
            public void onReply(String reply) throws RemoteException {
                Parcel data = Parcel.obtain();
                try {
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    XmlSerializer serializer = factory.newSerializer();
                    serializer.setOutput(data.asOutputStream(), "UTF-8");
                    serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                    serializer.startDocument("UTF-8", true);
                    serializer.startTag(null, "params");
                    serializer.startTag(null, "param");
                    serializer.startTag(null, "value");
                    serializer.startTag(null, "string");
                    serializer.text(reply);
                    serializer.endTag(null, "string");
                    serializer.endTag(null, "value");
                    serializer.endTag(null, "param");
                    serializer.endTag(null, "params");
                    serializer.endDocument();
                } catch (Exception e) {
                    throw new RemoteException("Binder transaction failure", e);
                }
                mRemote.transact(MSG_ON_REPLY, data, FLAG_ONEWAY);
            }
        }

        static final int MSG_ON_REPLY = 1;
    }
}
