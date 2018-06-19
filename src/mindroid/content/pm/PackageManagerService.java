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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import mindroid.app.Service;
import mindroid.content.ComponentName;
import mindroid.content.Context;
import mindroid.content.Intent;
import mindroid.os.Environment;
import mindroid.os.IBinder;
import mindroid.os.IServiceManager;
import mindroid.os.Process;
import mindroid.os.RemoteException;
import mindroid.os.ServiceManager;
import mindroid.util.Log;

public class PackageManagerService extends Service {
    private static final String LOG_TAG = "PackageManager";
    private static final String MANIFEST_TAG = "manifest";
    private static final String APPLICATION_TAG = "application";
    private static final String USES_LIBRARY_TAG = "uses-library";
    private static final String USES_PERMISSION_TAG = "uses-permission";
    private static final String SERVICE_TAG = "service";
    private static final String UTF_8 = "UTF-8";
    private Map<String, PackageInfo> mPackages = new LinkedHashMap<>();
    private Map<ComponentName, ComponentInfo> mComponents = new HashMap<>();
    private Map<String, Set<String>> mPermissions = new HashMap<>();

    @Override
    public void onCreate() {
        ServiceManager.addService(Context.PACKAGE_MANAGER, mManager);
        ServiceManager.addService(Context.PACKAGE_INSTALLER, mInstaller);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (PackageManager.ACTION_START_APPLICATIONS.equals(action)) {
            install(Environment.getAppsDirectory());
            List packages = getInstalledPackages(PackageManager.GET_SERVICES);
            if (packages != null) {
                IServiceManager serviceManager = ServiceManager.getServiceManager();
                for (Iterator itr = packages.iterator(); itr.hasNext();) {
                    PackageInfo p = (PackageInfo) itr.next();
                    if (p.services != null) {
                        ServiceInfo[] services = p.services;
                        for (int i = 0; i < services.length; i++) {
                            ServiceInfo serviceInfo = services[i];
                            if (serviceInfo.isEnabled() && serviceInfo.hasFlag(ServiceInfo.FLAG_AUTO_START)) {
                                Intent service = new Intent();
                                service.setComponent(new ComponentName(serviceInfo.packageName, serviceInfo.name));
                                try {
                                    serviceManager.startService(service);
                                } catch (RemoteException e) {
                                    throw new RuntimeException("System failure");
                                }
                            }
                        }
                    }
                }
            }
        } else if (PackageManager.ACTION_SHUTDOWN_APPLICATIONS.equals(action)) {
            List packages = getInstalledPackages(PackageManager.GET_SERVICES);
            if (packages != null) {
                IServiceManager serviceManager = ServiceManager.getServiceManager();
                for (Iterator itr = packages.iterator(); itr.hasNext();) {
                    PackageInfo p = (PackageInfo) itr.next();
                    if (p.services != null) {
                        ServiceInfo[] services = p.services;
                        for (int i = 0; i < services.length; i++) {
                            ServiceInfo serviceInfo = services[i];
                            if (serviceInfo.isEnabled()) {
                                Intent service = new Intent();
                                service.setComponent(new ComponentName(serviceInfo.packageName, serviceInfo.name));
                                try {
                                    serviceManager.stopService(service);
                                } catch (RemoteException ignore) {
                                }
                            }
                        }
                    }
                }
            }
        }

        return 0;
    }

    @Override
    public void onDestroy() {
        ServiceManager.removeService(mInstaller);
        ServiceManager.removeService(mManager);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mManager;
    }

    private final IPackageManager.Stub mManager = new IPackageManager.Stub() {
        @Override
        public List<PackageInfo> getInstalledPackages(int flags) throws RemoteException {
            return PackageManagerService.this.getInstalledPackages(flags);
        }

        @Override
        public PackageInfo getPackageInfo(String packageName, int flags) throws RemoteException {
            if ((flags & PackageManager.GET_SERVICES) == PackageManager.GET_SERVICES) {
                return mPackages.get(packageName);
            } else {
                return null;
            }
        }

        @Override
        public PackageInfo getPackageArchiveInfo(String archiveFilePath, int flags) throws RemoteException {
            if ((flags & PackageManager.GET_SERVICES) == PackageManager.GET_SERVICES) {
                File app = new File(archiveFilePath);
                if (app.exists()) {
                    return loadPackage(app);
                }
            }
            return null;
        }

        @Override
        public ResolveInfo resolveService(Intent intent, int flags) {
            ResolveInfo resolveInfo = null;
            if (mComponents.containsKey(intent.getComponent())) {
                ComponentInfo componentInfo = mComponents.get(intent.getComponent());
                if (componentInfo instanceof ServiceInfo) {
                    resolveInfo = new ResolveInfo();
                    resolveInfo.serviceInfo = (ServiceInfo) componentInfo;
                }
            }
            return resolveInfo;
        }

        @Override
        public int checkPermission(String permissionName, int pid) throws RemoteException {
            String process = Process.getName(pid);
            if (process != null) {
                Set<String> permissions = mPermissions.get(process);
                if (permissions != null && permissions.contains(permissionName)) {
                    return PackageManager.PERMISSION_GRANTED;
                }
            }
            return PackageManager.PERMISSION_DENIED;
        }

        @Override
        public String[] getPermissions(int pid) throws RemoteException {
            String process = Process.getName(pid);
            if (process != null) {
                Set<String> permissions = mPermissions.get(process);
                if (permissions != null) {
                    return (String[]) permissions.toArray(new String[permissions.size()]);
                }
            }
            return new String[] {};
        }
    };

    private final IPackageInstaller.Stub mInstaller = new IPackageInstaller.Stub() {
        @Override
        public void install(File file) throws RemoteException {
            PackageManagerService.this.install(file);
        }

        @Override
        public void uninstall(String packageName) throws RemoteException {
            removePackage(packageName);
        }
    };

    private List<PackageInfo> getInstalledPackages(int flags) {
        if ((flags & PackageManager.GET_SERVICES) == PackageManager.GET_SERVICES) {
            ArrayList<PackageInfo> packages = new ArrayList<>();
            Iterator<Map.Entry<String, PackageInfo>> itr = mPackages.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, PackageInfo> entry = itr.next();
                PackageInfo p = entry.getValue();
                packages.add(p);
            }
            return packages.isEmpty() ? null : packages;
        } else {
            return null;
        }
    }

    private void install(File file) {
        if (file.isDirectory()) {
            final File directiry = file;
            File[] apps = directiry.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".jar");
                }
            });

            if (apps != null) {
                Arrays.sort(apps);
                for (int i = 0; i < apps.length; i++) {
                    PackageInfo packageInfo = loadPackage(apps[i]);
                    if (packageInfo != null) {
                        if (!mPackages.containsKey(packageInfo.packageName)) {
                            addPackage(packageInfo);
                        }
                    }
                }
            }
        } else {
            final File app = file;
            if (app.exists()) {
                PackageInfo packageInfo = loadPackage(app);
                if (packageInfo != null) {
                    if (!mPackages.containsKey(packageInfo.packageName)) {
                        addPackage(packageInfo);
                    }
                }
            }
        }
    }

    private void addPackage(PackageInfo packageInfo) {
        mPackages.put(packageInfo.packageName, packageInfo);

        for (int i = 0; i < packageInfo.services.length; i++) {
            ServiceInfo si = (ServiceInfo) packageInfo.services[i];
            mComponents.put(new ComponentName(si.packageName, si.name), si);
        }

        if (packageInfo.permissions != null) {
            final String processName = packageInfo.applicationInfo.processName;
            if (!mPermissions.containsKey(processName)) {
                mPermissions.put(processName, new HashSet<>());
            }
            Set<String> permissions = mPermissions.get(processName);
            permissions.addAll(Arrays.asList(packageInfo.permissions));
        }
    }

    private void removePackage(final String packageName) {
        PackageInfo packageInfo = mPackages.get(packageName);
        if (packageInfo != null) {
            mPackages.remove(packageName);

            for (int i = 0; i < packageInfo.services.length; i++) {
                ServiceInfo si = (ServiceInfo) packageInfo.services[i];
                mComponents.remove(new ComponentName(si.packageName, si.name));
            }

            if (packageInfo.permissions != null) {
                final String processName = packageInfo.applicationInfo.processName;
                if (mPermissions.containsKey(processName)) {
                    Set<String> permissions = mPermissions.get(processName);
                    permissions.removeAll(Arrays.asList(packageInfo.permissions));
                }
            }
        }
    }

    private static PackageInfo loadPackage(File app) {
        ZipInputStream inputStream = null;
        try {
            inputStream = new ZipInputStream(new FileInputStream(app));
            ZipEntry entry = inputStream.getNextEntry();

            while (entry != null) {
                String fileName = entry.getName();
                if (fileName.equals("MindroidManifest.xml")) {
                    return parseManifest(app, inputStream);
                } else {
                    entry = inputStream.getNextEntry();
                }
            }
        } catch (XmlPullParserException e) {
            Log.e(LOG_TAG, "Cannot read manifest file in " + app.getPath(), e);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Cannot read manifest file in " + app.getPath(), e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }

        return null;
    }

    private static PackageInfo parseManifest(File app, InputStream input) throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput((InputStream) input, UTF_8);
        parser.require(XmlPullParser.START_DOCUMENT, null, null);
        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, MANIFEST_TAG);

        PackageInfo pi = new PackageInfo();
        ApplicationInfo ai = new ApplicationInfo();
        pi.applicationInfo = ai;
        ai.fileName = app.getAbsolutePath();

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
        pi.packageName = packageName;
        ai.packageName = pi.packageName;

        List<ServiceInfo> services = new ArrayList<>();
        boolean applicationTagDone = false;
        for (int eventType = parser.nextTag(); !parser.getName().equals(MANIFEST_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
            if (parser.getName().equals(APPLICATION_TAG)) {
                if (applicationTagDone) {
                    throw new XmlPullParserException("Only one application is allowed per manifest");
                }
                services = parseApplication(parser, pi);
                pi.services = (ServiceInfo[]) services.toArray(new ServiceInfo[services.size()]);
                applicationTagDone = true;
            } else {
                String tag = parser.getName();
                skipSubTree(parser);
                parser.require(XmlPullParser.END_TAG, null, tag);
            }
        }

        parser.require(XmlPullParser.END_TAG, null, MANIFEST_TAG);
        parser.next();
        parser.require(XmlPullParser.END_DOCUMENT, null, null);

        return pi;
    }

    private static List<ServiceInfo> parseApplication(XmlPullParser parser, PackageInfo pi) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, APPLICATION_TAG);

        ApplicationInfo ai = pi.applicationInfo;
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

        List<String> libraries = new ArrayList<>();
        List<String> permissions = new ArrayList<>();
        List<ServiceInfo> services = new ArrayList<>();
        for (int eventType = parser.nextTag(); !parser.getName().equals(APPLICATION_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
            if (parser.getName().equals(USES_LIBRARY_TAG)) {
                String library = parseLibrary(parser);
                if (library != null) {
                    libraries.add(library);
                }
            } else if (parser.getName().equals(USES_PERMISSION_TAG)) {
                String permission = parsePermission(parser);
                if (permission != null) {
                    permissions.add(permission);
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
                skipSubTree(parser);
                parser.require(XmlPullParser.END_TAG, null, tag);
            }
        }

        if (!libraries.isEmpty()) {
            ai.libraries = (String[]) libraries.toArray(new String[libraries.size()]);
        }
        if (!permissions.isEmpty()) {
            pi.permissions = (String[]) permissions.toArray(new String[permissions.size()]);
        }

        parser.require(XmlPullParser.END_TAG, null, APPLICATION_TAG);

        return services;
    }

    private static String parseLibrary(XmlPullParser parser) throws IOException, XmlPullParserException {
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
            skipSubTree(parser);
            parser.require(XmlPullParser.END_TAG, null, tag);
        }

        parser.require(XmlPullParser.END_TAG, null, USES_LIBRARY_TAG);

        if (name != null) {
            return new File(Environment.getAppsDirectory() + File.separator + name).getAbsolutePath();
        } else {
            return null;
        }
    }

    private static String parsePermission(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, USES_PERMISSION_TAG);

        String name = null;
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            String attributeName = parser.getAttributeName(i);
            String attributeValue = parser.getAttributeValue(i);
            if (attributeName.equals("name")) {
                name = attributeValue;
            }
        }

        for (int eventType = parser.nextTag(); !parser.getName().equals(USES_PERMISSION_TAG) && eventType != XmlPullParser.END_TAG; eventType = parser.nextTag()) {
            String tag = parser.getName();
            skipSubTree(parser);
            parser.require(XmlPullParser.END_TAG, null, tag);
        }

        parser.require(XmlPullParser.END_TAG, null, USES_PERMISSION_TAG);
        return name;
    }

    private static ServiceInfo parseService(XmlPullParser parser, ApplicationInfo ai) throws IOException, XmlPullParserException {
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
            skipSubTree(parser);
            parser.require(XmlPullParser.END_TAG, null, tag);
        }

        parser.require(XmlPullParser.END_TAG, null, SERVICE_TAG);
        return si;
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
}
