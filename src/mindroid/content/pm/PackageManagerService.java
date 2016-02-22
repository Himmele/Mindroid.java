/*
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import mindroid.app.Service;
import mindroid.content.ComponentName;
import mindroid.content.Context;
import mindroid.content.Intent;
import mindroid.os.Environment;
import mindroid.os.Handler;
import mindroid.os.IBinder;
import mindroid.os.Message;
import mindroid.os.RemoteException;
import mindroid.os.ServiceManager;
import mindroid.util.Log;

public class PackageManagerService extends Service {
	private static final String LOG_TAG = "PackageManager";
	private static final String MANIFEST_TAG = "manifest";
	private static final String APPLICATION_TAG = "application";
	private static final String USES_LIBRARY_TAG = "uses-library";
	private static final String SERVICE_TAG = "service";
	private static final int MSG_ADD_PACKAGE = 1;
	private static final int MSG_BOOT_COMPLETED = 2;
	private static final String UTF_8 = "UTF-8";
	private Thread mThread = null;
	private HashMap mPackages = new HashMap();
	private HashMap mComponents = new HashMap();
	private List mListeners = new ArrayList();
	private boolean mBootCompleted = false;

	public void onCreate() {
		ServiceManager.addService(Context.PACKAGE_MANAGER, mBinder);
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		if (mThread == null) {
			mThread = new Thread(mScanner, LOG_TAG);
			mThread.start();
		}
		return 0;
	}

	public void onDestroy() {
		ServiceManager.removeService(Context.PACKAGE_MANAGER);
	}

	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	private final IPackageManager.Stub mBinder = new IPackageManager.Stub() {
		public List getInstalledPackages(int flags) throws RemoteException {
			if ((flags & PackageManager.GET_SERVICES) == PackageManager.GET_SERVICES) {
				ArrayList packages = new ArrayList();
				Iterator itr = mPackages.entrySet().iterator();
				while (itr.hasNext()) {
					Map.Entry entry = (Map.Entry) itr.next();
					PackageInfo p = (PackageInfo) entry.getValue();
					packages.add(p);
				}
				return packages.isEmpty() ? null : packages;
			} else {
				return null;
			}
		}

		public ResolveInfo resolveService(Intent intent, int flags) {
			ResolveInfo resolveInfo = null;
			ComponentInfo componentInfo = (ComponentInfo) mComponents.get(intent.getComponent());
			if (componentInfo != null && componentInfo instanceof ServiceInfo) {
				resolveInfo = new ResolveInfo();
				resolveInfo.serviceInfo = (ServiceInfo) componentInfo;
			}
			return resolveInfo;
		}

		public void addListener(IPackageManagerListener listener) {
			if (!mListeners.contains(listener)) {
				mListeners.add(listener);

				if (mBootCompleted) {
					try {
						listener.onBootCompleted();
					} catch (RemoteException e) {
						removeListener(listener);
					}
				}
			}
		}

		public void removeListener(IPackageManagerListener listener) {
			mListeners.remove(listener);
		}
	};

	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_ADD_PACKAGE:
				PackageInfo packageInfo = (PackageInfo) msg.obj;
				mPackages.put(packageInfo.packageName, packageInfo);

				for (int i = 0; i < packageInfo.services.length; i++) {
					ServiceInfo si = (ServiceInfo) packageInfo.services[i];
					mComponents.put(new ComponentName(si.packageName, si.name), si);
				}
				break;
			case MSG_BOOT_COMPLETED:
				onBootCompleted();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	};

	void bootCompleted() {
		Message msg = mHandler.obtainMessage(MSG_BOOT_COMPLETED);
		msg.sendToTarget();
	}

	private void onBootCompleted() {
		mBootCompleted = true;

		for (Iterator itr = mListeners.iterator(); itr.hasNext();) {
			IPackageManagerListener listener = (IPackageManagerListener) itr.next();
			try {
				listener.onBootCompleted();
			} catch (RemoteException e) {
				itr.remove();
			}
		}
	}

	private Runnable mScanner = new Runnable() {
		public void run() {
			File[] apps = Environment.getAppsDirectory().listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(".jar");
				}
			});

			if (apps != null) {
				for (int i = 0; i < apps.length; i++) {
					ZipInputStream inputStream = null;
					try {
						inputStream = new ZipInputStream(new FileInputStream(apps[i]));
						ZipEntry entry = inputStream.getNextEntry();

						while (entry != null) {
							String fileName = entry.getName();
							if (fileName.equals("MindroidManifest.xml")) {
								ApplicationInfo ai = new ApplicationInfo();
								ai.fileName = apps[i].getAbsolutePath();
								parseManifest(inputStream, ai);
								break;
							} else {
								entry = inputStream.getNextEntry();
							}
						}
					} catch (XmlPullParserException e) {
						Log.e(LOG_TAG, "Cannot read manifest file in " + apps[i].getPath(), e);
					} catch (IOException e) {
						Log.e(LOG_TAG, "Cannot read manifest file in " + apps[i].getPath(), e);
					} finally {
						if (inputStream != null) {
							try {
								inputStream.close();
							} catch (IOException e) {
							}
						}
					}
				}
			}

			bootCompleted();
			mThread = null;
		}
	};

	public void parseManifest(InputStream input, ApplicationInfo ai) throws XmlPullParserException, IOException {
		KXmlParser parser;
		parser = new KXmlParser();
		parser.setInput((InputStream) input, UTF_8);
		parser.setFeature(KXmlParser.FEATURE_PROCESS_NAMESPACES, true);
		parser.require(XmlPullParser.START_DOCUMENT, null, null);
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, MANIFEST_TAG);

		String packageName = null;
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			String attributeName = parser.getAttributeName(i);
			String attributeValue = parser.getAttributeValue(i);
			if (attributeName.equals("package")) {
				packageName = attributeValue;
			}
		}
		if (packageName == null || packageName.length() == 0) {
			throw new XmlPullParserException("Manifest is missing a package name");
		}
		ai.packageName = packageName;

		List services = null;
		boolean applicationTagDone = false;
		for (int eventType = parser.nextTag(); !parser.getName().equals(MANIFEST_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
			if (parser.getName().equals(APPLICATION_TAG)) {
				if (applicationTagDone) {
					throw new XmlPullParserException("Only one application is allowed per manifest");
				}
				services = parseApplication(parser, ai);
				applicationTagDone = true;
			} else {
				String tag = parser.getName();
				parser.skipSubTree();
				parser.require(XmlPullParser.END_TAG, null, tag);
			}
		}

		parser.require(XmlPullParser.END_TAG, null, MANIFEST_TAG);
		parser.next();
		parser.require(XmlPullParser.END_DOCUMENT, null, null);

		if (services != null && !services.isEmpty()) {
			PackageInfo packageInfo = new PackageInfo();
			packageInfo.packageName = packageName;
			packageInfo.applicationInfo = ai;
			packageInfo.services = (ServiceInfo[]) services.toArray(new ServiceInfo[services.size()]);
			mHandler.obtainMessage(MSG_ADD_PACKAGE, packageInfo).sendToTarget();
		}
	}

	List parseApplication(KXmlParser parser, ApplicationInfo ai) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, APPLICATION_TAG);

		String processName = ai.packageName;
		boolean enabled = true;
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			String attributeName = parser.getAttributeName(i);
			String attributeValue = parser.getAttributeValue(i);
			if (attributeName.equals("process")) {
				processName = attributeValue;
			} else if (attributeName.equals("enabled")) {
				if (attributeValue.equals("true")) {
					enabled = true;
				} else if (attributeValue.equals("false")) {
					enabled = false;
				} else {
					throw new XmlPullParserException("Unknwon value for application attribute 'enabled'");
				}
			}
		}
		ai.processName = processName;
		ai.enabled = enabled;

		List libraries = new ArrayList();
		List services = new ArrayList();
		for (int eventType = parser.nextTag(); !parser.getName().equals(APPLICATION_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
			if (parser.getName().equals(USES_LIBRARY_TAG)) {
				String library = parseLibrary(parser);
				if (library != null) {
					libraries.add(library);
				}
			} else if (parser.getName().equals(SERVICE_TAG)) {
				ServiceInfo si = parseService(parser, ai);
				if (si != null) {
					if (si.name != null && si.name.length() > 0) {
						services.add(si);
					} else {
						Log.w(LOG_TAG, "Invalid name for component " + si.name);
					}
				}
			} else {
				String tag = parser.getName();
				parser.skipSubTree();
				parser.require(XmlPullParser.END_TAG, null, tag);
			}
		}

		if (!libraries.isEmpty()) {
			ai.libraries = (String[]) libraries.toArray(new String[libraries.size()]);
		}

		parser.require(XmlPullParser.END_TAG, null, APPLICATION_TAG);

		return services;
	}

	String parseLibrary(KXmlParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, USES_LIBRARY_TAG);

		String name = null;
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			String attributeName = parser.getAttributeName(i);
			String attributeValue = parser.getAttributeValue(i);
			if (attributeName.equals("name")) {
				name = attributeValue + ".jar";
			}
		}

		for (int eventType = parser.nextTag(); !parser.getName().equals(USES_LIBRARY_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
			String tag = parser.getName();
			parser.skipSubTree();
			parser.require(XmlPullParser.END_TAG, null, tag);
		}

		parser.require(XmlPullParser.END_TAG, null, USES_LIBRARY_TAG);

		if (name != null) {
			return new File(Environment.getAppsDirectory() + File.separator + name).getAbsolutePath();
		} else {
			return null;
		}
	}

	ServiceInfo parseService(KXmlParser parser, ApplicationInfo ai) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, SERVICE_TAG);

		String processName = ai.processName;
		String name = null;
		boolean enabled = true;
		boolean autostart = false;
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			String attributeName = parser.getAttributeName(i);
			String attributeValue = parser.getAttributeValue(i);

			if (attributeName.equals("name")) {
				name = attributeValue;
				if (name.startsWith(".")) {
					try {
						name = name.substring(1);
					} catch (IndexOutOfBoundsException e) {
						name = null;
					}
				} else {
					if (name.startsWith(ai.packageName)) {
						try {
							name = name.substring(ai.packageName.length() + 1);
						} catch (IndexOutOfBoundsException e) {
							name = null;
						}
					} else {
						throw new XmlPullParserException("Invalid name " + name);
					}
				}
			} else if (attributeName.equals("process")) {
				processName = attributeValue;
			} else if (attributeName.equals("enabled")) {
				if (attributeValue.equals("true")) {
					enabled = true;
				} else if (attributeValue.equals("false")) {
					enabled = false;
				} else {
					throw new XmlPullParserException("Unknwon value for service attribute 'enabled'");
				}
			} else if (attributeName.equals("autostart")) {
				if (attributeValue.equals("true")) {
					autostart = true;
				} else if (attributeValue.equals("false")) {
					autostart = false;
				} else {
					throw new XmlPullParserException("Unknown value for service attribute 'autostart'");
				}
			}
		}

		if (name == null || name.length() == 0) {
			throw new XmlPullParserException("Invalid name");
		}

		ServiceInfo si = new ServiceInfo();
		si.name = name;
		si.packageName = ai.packageName;
		si.applicationInfo = ai;
		si.processName = processName;
		si.enabled = enabled;
		if (autostart) {
			si.flags |= ServiceInfo.FLAG_AUTO_START;
		}

		for (int eventType = parser.nextTag(); !parser.getName().equals(SERVICE_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
			String tag = parser.getName();
			parser.skipSubTree();
			parser.require(XmlPullParser.END_TAG, null, tag);
		}

		parser.require(XmlPullParser.END_TAG, null, SERVICE_TAG);
		return si;
	}
}
