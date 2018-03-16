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

import java.net.URI;
import mindroid.os.Binder;
import mindroid.os.IBinder;
import mindroid.os.Parcel;
import mindroid.os.RemoteException;
import mindroid.util.concurrent.Future;
import mindroid.util.concurrent.Promise;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

public interface IEliza extends examples.eliza.IEliza {
    public static class Stub extends Binder {
        private static final String DESCRIPTOR = "xmlrpc://interfaces/examples/eliza/IEliza";
        private final examples.eliza.IEliza mEliza;

        public Stub(Binder binder) {
            super(binder);
            mEliza = (examples.eliza.IEliza) binder.queryLocalInterface(binder.getInterfaceDescriptor());
            this.attachInterface(mEliza, DESCRIPTOR);
        }

        @Override
        protected void onTransact(int what, Parcel data, Promise<Parcel> result) throws RemoteException {
            switch (what) {
            case MSG_ASK1: {
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
                    String question = parser.nextText();
                    parser.require(XmlPullParser.END_TAG, null, "string");
                    parser.nextTag();
                    parser.require(XmlPullParser.END_TAG, null, "value");
                    parser.nextTag();
                    parser.require(XmlPullParser.END_TAG, null, "param");
                    parser.nextTag();
                    parser.require(XmlPullParser.END_TAG, null, "params");
                    parser.next();
                    parser.require(XmlPullParser.END_DOCUMENT, null, null);

                    final String reply = mEliza.ask1(question);
                    Parcel parcel = Parcel.obtain();
                    try {
                        XmlSerializer serializer = factory.newSerializer();
                        serializer.setOutput(parcel.asOutputStream(), "UTF-8");
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
                        result.complete(parcel);
                    } catch (Exception e) {
                        result.completeWith(e);
                    }
                } catch (Exception e) {
                    result.completeWith(e);
                }
                break;
            }
            case MSG_ASK2: {
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
                    String question = parser.nextText();
                    parser.require(XmlPullParser.END_TAG, null, "string");
                    parser.nextTag();
                    parser.require(XmlPullParser.END_TAG, null, "value");
                    parser.nextTag();
                    parser.require(XmlPullParser.END_TAG, null, "param");
                    parser.nextTag();
                    parser.require(XmlPullParser.END_TAG, null, "params");
                    parser.next();
                    parser.require(XmlPullParser.END_DOCUMENT, null, null);

                    final Future<String> reply = mEliza.ask2(question);
                    reply.then(value -> {
                        Parcel parcel = Parcel.obtain();
                        try {
                            XmlSerializer serializer = factory.newSerializer();
                            serializer.setOutput(parcel.asOutputStream(), "UTF-8");
                            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                            serializer.startDocument("UTF-8", true);
                            serializer.startTag(null, "params");
                            serializer.startTag(null, "param");
                            serializer.startTag(null, "value");
                            serializer.startTag(null, "string");
                            serializer.text(reply.get());
                            serializer.endTag(null, "string");
                            serializer.endTag(null, "value");
                            serializer.endTag(null, "param");
                            serializer.endTag(null, "params");
                            serializer.endDocument();
                            result.complete(parcel);
                        } catch (Exception e) {
                            result.completeWith(e);
                        }
                    });
                } catch (Exception e) {
                    result.completeWith(e);
                }
                break;
            }
            case MSG_ASK3: {
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
                    String question = parser.nextText();
                    parser.require(XmlPullParser.END_TAG, null, "string");
                    parser.nextTag();
                    parser.require(XmlPullParser.START_TAG, null, "binder");
                    IBinder binder = Parcel.fromUri(URI.create(parser.nextText()));
                    parser.require(XmlPullParser.END_TAG, null, "binder");
                    parser.nextTag();
                    parser.require(XmlPullParser.END_TAG, null, "value");
                    parser.nextTag();
                    parser.require(XmlPullParser.END_TAG, null, "param");
                    parser.nextTag();
                    parser.require(XmlPullParser.END_TAG, null, "params");
                    parser.next();
                    parser.require(XmlPullParser.END_DOCUMENT, null, null);

                    mEliza.ask3(question, examples.eliza.IElizaListener.Stub.asInterface(binder));
                } catch (Exception e) {
                    result.completeWith(e);
                }
                break;
            }
            default:
                super.onTransact(what, data, result);
                break;
            }
        }

        public static class Proxy implements IEliza {
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
            public String ask1(String question) throws RemoteException {
                Promise<String> promise = new Promise<>();
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
                    serializer.text(question);
                    serializer.endTag(null, "string");
                    serializer.endTag(null, "value");
                    serializer.endTag(null, "param");
                    serializer.endTag(null, "params");
                    serializer.endDocument();
                } catch (Exception e) {
                    throw new RemoteException("Binder transaction failure", e);
                }
                mRemote.transact(MSG_ASK1, data, 0)
                    .then((parcel, exception) -> {
                        if (exception == null) {
                            try {
                                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                                factory.setNamespaceAware(true);
                                XmlPullParser parser = factory.newPullParser();
                                parser.setInput(parcel.asInputStream(), "UTF-8");
                                parser.require(XmlPullParser.START_DOCUMENT, null, null);
                                parser.nextTag();
                                parser.require(XmlPullParser.START_TAG, null, "params");
                                parser.nextTag();
                                parser.require(XmlPullParser.START_TAG, null, "param");
                                parser.nextTag();
                                parser.require(XmlPullParser.START_TAG, null, "value");
                                parser.nextTag();
                                parser.require(XmlPullParser.START_TAG, null, "string");
                                String reply = parser.nextText();
                                parser.require(XmlPullParser.END_TAG, null, "string");
                                parser.nextTag();
                                parser.require(XmlPullParser.END_TAG, null, "value");
                                parser.nextTag();
                                parser.require(XmlPullParser.END_TAG, null, "param");
                                parser.nextTag();
                                parser.require(XmlPullParser.END_TAG, null, "params");
                                parser.next();
                                parser.require(XmlPullParser.END_DOCUMENT, null, null);
                                promise.complete(reply);
                            } catch (Exception e) {
                                promise.completeWith(e);
                            }
                        } else {
                            promise.completeWith(exception);
                        }
                    });
                return Binder.get(promise);
            }

            @Override
            public Future<String> ask2(String question) throws RemoteException {
                Promise<String> promise = new Promise<>();
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
                    serializer.text(question);
                    serializer.endTag(null, "string");
                    serializer.endTag(null, "value");
                    serializer.endTag(null, "param");
                    serializer.endTag(null, "params");
                    serializer.endDocument();
                } catch (Exception e) {
                    throw new RemoteException("Binder transaction failure", e);
                }
                mRemote.transact(MSG_ASK2, data, 0)
                    .then((parcel, exception) -> {
                        if (exception == null) {
                            try {
                                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                                factory.setNamespaceAware(true);
                                XmlPullParser parser = factory.newPullParser();
                                parser.setInput(parcel.asInputStream(), "UTF-8");
                                parser.require(XmlPullParser.START_DOCUMENT, null, null);
                                parser.nextTag();
                                parser.require(XmlPullParser.START_TAG, null, "params");
                                parser.nextTag();
                                parser.require(XmlPullParser.START_TAG, null, "param");
                                parser.nextTag();
                                parser.require(XmlPullParser.START_TAG, null, "value");
                                parser.nextTag();
                                parser.require(XmlPullParser.START_TAG, null, "string");
                                String reply = parser.nextText();
                                parser.require(XmlPullParser.END_TAG, null, "string");
                                parser.nextTag();
                                parser.require(XmlPullParser.END_TAG, null, "value");
                                parser.nextTag();
                                parser.require(XmlPullParser.END_TAG, null, "param");
                                parser.nextTag();
                                parser.require(XmlPullParser.END_TAG, null, "params");
                                parser.next();
                                parser.require(XmlPullParser.END_DOCUMENT, null, null);
                                promise.complete(reply);
                            } catch (Exception e) {
                                promise.completeWith(e);
                            }
                        } else {
                            promise.completeWith(exception);
                        }
                    });
                return promise;
            }

            @Override
            public void ask3(String question, examples.eliza.IElizaListener listener) throws RemoteException {
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
                    serializer.text(question);
                    serializer.endTag(null, "string");
                    serializer.startTag(null, "binder");
                    serializer.text(Parcel.toUri(mRemote, listener.asBinder()).toString());
                    serializer.endTag(null, "binder");
                    serializer.endTag(null, "value");
                    serializer.endTag(null, "param");
                    serializer.endTag(null, "params");
                    serializer.endDocument();
                } catch (Exception e) {
                    throw new RemoteException("Binder transaction failure", e);
                }
                mRemote.transact(MSG_ASK3, data, FLAG_ONEWAY);
            }
        }

        static final int MSG_ASK1 = 1;
        static final int MSG_ASK2 = 2;
        static final int MSG_ASK3 = 3;
    }
}
