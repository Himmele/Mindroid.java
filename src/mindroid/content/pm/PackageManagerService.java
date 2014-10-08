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
import java.net.URL;
import java.net.URLClassLoader;
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
	private static final int MSG_PUBLISH_SERVICES = 1;
	private static final int MSG_BOOT_COMPLETED = 2;
	private static final String UTF_8 = "UTF-8";
	private final File mAppDir = Environment.getAppsDirectory();
	private Thread mThread = null;
	private HashMap mServices = new HashMap();
	private List mListeners = new ArrayList();
	private PackageHandler mHandler;
	private boolean mBootCompleted = false;

	public void onCreate() {
		ServiceManager.addService(Context.PACKAGE_MANAGER, mBinder);
		
		mHandler = new PackageHandler();
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
		public ServiceInfo resolveService(Intent service) {
			return (ServiceInfo) mServices.get(service.getComponent());
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
			int i;
			for (i = 0; i < mListeners.size(); i++) {
				if (((IPackageManagerListener) mListeners.get(i)).asBinder() == listener.asBinder()) {
					break;
				}
			}
			if (i < mListeners.size()) {
				mListeners.remove(i);
			}
		}

		public ComponentName[] getAutostartServices() {
			List components = new ArrayList();
			Iterator itr = mServices.entrySet().iterator();
		    while (itr.hasNext()) {
		        Map.Entry service = (Map.Entry) itr.next();
		        ServiceInfo serviceInfo = (ServiceInfo) service.getValue();
		        if (serviceInfo.enabled && serviceInfo.autostart) {
		        	components.add(service.getKey());
		        }
		    }
		    return (ComponentName[]) components.toArray(new ComponentName[components.size()]);
		}
	};
	
	class PackageHandler extends Handler {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_PUBLISH_SERVICES:
				List services = (List) msg.obj;
				for (int i = 0; i < services.size(); i++) {
					ServiceInfo si = (ServiceInfo) services.get(i);
					String serviceName = si.serviceName;
					String packageName = serviceName.substring(0, serviceName.lastIndexOf("."));
					String className = serviceName.substring(serviceName.lastIndexOf(".") + 1);
					mServices.put(new ComponentName(packageName, className), si);
				}
				break;
			case MSG_BOOT_COMPLETED:
				onBootCompleted();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	void bootCompleted() {
		Message msg = mHandler.obtainMessage(MSG_BOOT_COMPLETED);
		msg.sendToTarget();
	}
	
	private void onBootCompleted() {
		mBootCompleted = true;
		
		ArrayList deadListeners = null;
		for (Iterator itr = mListeners.iterator(); itr.hasNext();) {
			IPackageManagerListener listener = (IPackageManagerListener) itr.next();
			try {
				listener.onBootCompleted();
			} catch (RemoteException e) {
				if (deadListeners == null) {
					deadListeners = new ArrayList();
				}
				deadListeners.add(listener);
			}
		}
		
		if (deadListeners != null) {
			for (Iterator itr = deadListeners.iterator(); itr.hasNext();) {
				mBinder.removeListener((IPackageManagerListener) itr.next());
			}
		}
	}
	
	private Runnable mScanner = new Runnable() {
		public void run() {
			File[] apps = mAppDir.listFiles(new FilenameFilter() {
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
	
					} catch (IOException ex) {
						Log.e(LOG_TAG, "Cannot read manifest file in jar");
					} finally {
						if (inputStream != null) {
							try {
								inputStream.close();
							} catch(IOException e) {
							}
						}
					}
				}
			}
			
			bootCompleted();
			mThread = null;
		}
	};
	
	public void parseManifest(InputStream input, ApplicationInfo ai) {
		try {
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
			if (packageName == null) {
				throw new XmlPullParserException("Manifest is missing a package name");
			}
			ai.packageName = packageName;
			
			List services = null;
			boolean applicationTagDone = false;
			for (int eventType = parser.nextTag(); !parser.getName().equals(MANIFEST_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
				if (eventType == XmlPullParser.END_TAG) {
					throw new XmlPullParserException("Invalid XML format");
				}
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
			
			if (services != null) {
				mHandler.obtainMessage(MSG_PUBLISH_SERVICES, services).sendToTarget();
			}
		} catch (Exception e) {
			Log.e(LOG_TAG, "XML parsing error", e);
		}
	}
	
	List parseApplication(KXmlParser parser, ApplicationInfo ai) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, APPLICATION_TAG);
		
		String appProcessName = ai.packageName;
		boolean appEnabled = true;
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			String attributeName = parser.getAttributeName(i);
			String attributeValue = parser.getAttributeValue(i);
			if (attributeName.equals("process")) {
				appProcessName = attributeValue;
			} else if (attributeName.equals("enabled")) {
				if (attributeValue.equals("true")) {
					appEnabled = true;
				} else if (attributeValue.equals("false")) {
					appEnabled = false;
				} else {
					throw new XmlPullParserException("Unknwon value for application attribute 'enabled'");
				}
			}
		}
		ai.processName = appProcessName;
		ai.enabled = appEnabled;
		
		List libraries = new ArrayList();
		List services = new ArrayList();
		for (int eventType = parser.nextTag(); !parser.getName().equals(APPLICATION_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
			if (eventType == XmlPullParser.END_TAG) {
				throw new XmlPullParserException("Invalid XML format");
			}
			if (parser.getName().equals(USES_LIBRARY_TAG)) {
				String library = parseLibrary(parser);
				if (library != null) {
					libraries.add(library);
				}
			} else if (parser.getName().equals(SERVICE_TAG)) {
				ServiceInfo si = parseService(parser, ai);
				if (si != null) {
					services.add(si);
				}
			} else {
				String tag = parser.getName();
				parser.skipSubTree();
				parser.require(XmlPullParser.END_TAG, null, tag);
			}
		}
		
		parser.require(XmlPullParser.END_TAG, null, APPLICATION_TAG);
		
		URL[] urls = new URL[libraries.size() + 1];
		if (libraries.size() > 0) {
			ai.libraries = (String[]) libraries.toArray(new String[libraries.size()]);
			for (int i = 0; i < ai.libraries.length; i++) {
				urls[i] = new File(ai.libraries[i]).toURI().toURL();
			}
		}

		for (Iterator itr = services.iterator(); itr.hasNext();) {
			ServiceInfo serviceInfo = (ServiceInfo) itr.next();

			urls[urls.length - 1] = new File(ai.fileName).toURI().toURL();
			boolean classNotFound = false;
			URLClassLoader classLoader = new URLClassLoader(urls, getClass().getClassLoader());
			try {
				Class clazz = Class.forName(serviceInfo.serviceName, false, classLoader);
			} catch (Exception e) {
				Log.e(LOG_TAG, "Cannot find service " + serviceInfo.serviceName + " in file " + ai.fileName, e);
				classNotFound = true;
			} catch (LinkageError e) {
				Log.e(LOG_TAG, "Linkage error: " + e.getMessage(), e);
				classNotFound = true;
			}
			if (classNotFound) {
				itr.remove();
			}
		}

		return services;
	}
	
	String parseLibrary(KXmlParser parser) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, USES_LIBRARY_TAG);

		String libraryName = null;
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			String attributeName = parser.getAttributeName(i);
			String attributeValue = parser.getAttributeValue(i);
			if (attributeName.equals("name")) {
				libraryName = attributeValue + ".jar";
			}
		}
		
		for (int eventType = parser.nextTag(); !parser.getName().equals(USES_LIBRARY_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
			if (eventType == XmlPullParser.END_TAG) {
				throw new XmlPullParserException("Invalid XML format");
			}
			String tag = parser.getName();
			parser.skipSubTree();
			parser.require(XmlPullParser.END_TAG, null, tag);
		}
		
		parser.require(XmlPullParser.END_TAG, null, USES_LIBRARY_TAG);
		
		if (libraryName != null) {
			return new File(Environment.getAppsDirectory() + File.separator + libraryName).getAbsolutePath();
		} else {
			return null;
		}
	}
	
	ServiceInfo parseService(KXmlParser parser, ApplicationInfo ai) throws IOException, XmlPullParserException {
		parser.require(XmlPullParser.START_TAG, null, SERVICE_TAG);
		
		String processName = ai.processName;
		String serviceName = null;
		boolean enabled = true;
		boolean autostart = false;
		for (int i = 0; i < parser.getAttributeCount(); i++) {
			String attributeName = parser.getAttributeName(i);
			String attributeValue = parser.getAttributeValue(i);
			
			if (attributeName.equals("name")) {
				serviceName = attributeValue;
				if (serviceName.startsWith(".")) {
					serviceName = ai.packageName + serviceName; 
				} else {
					if (!serviceName.startsWith(ai.packageName)) {
						throw new XmlPullParserException("Invalid serviceName " + serviceName);
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

		if (serviceName == null) {
			throw new XmlPullParserException("Undefined serviceName");
		}

		ServiceInfo si = new ServiceInfo();
		si.applicationInfo = ai;
		si.processName = processName;
		si.serviceName = serviceName;
		si.enabled = enabled;
		si.autostart = autostart;

		for (int eventType = parser.nextTag(); !parser.getName().equals(SERVICE_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
			if (eventType == XmlPullParser.END_TAG) {
				throw new XmlPullParserException("Invalid XML format");
			}
			String tag = parser.getName();
			parser.skipSubTree();
			parser.require(XmlPullParser.END_TAG, null, tag);
		}
		
		parser.require(XmlPullParser.END_TAG, null, SERVICE_TAG);
		return si;
	}
}
