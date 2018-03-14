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

package main;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import mindroid.content.ComponentName;
import mindroid.content.Context;
import mindroid.content.Intent;
import mindroid.content.pm.IPackageManager;
import mindroid.content.pm.PackageInfo;
import mindroid.content.pm.PackageManager;
import mindroid.content.pm.PackageManagerListener;
import mindroid.content.pm.ServiceInfo;
import mindroid.os.Environment;
import mindroid.os.IServiceManager;
import mindroid.os.Looper;
import mindroid.os.RemoteException;
import mindroid.os.ServiceManager;
import mindroid.util.Log;
import mindroid.util.logging.Logger;
import mindroid.runtime.system.Runtime;

public class Main {
    private static final String LOG_TAG = "Mindroid";
    private static final String ID_ARG = "id=";
    private static final String ROOT_DIR_ARG = "rootDir=";

    /**
     * Linux: java -classpath Mindroid.jar:Main.jar main.Main rootDir=.
     * Microsoft Windows: java -classpath Mindroid.jar;Main.jar main.Main rootDir=.
     */
    public static void main(String[] args) {
        int nodeId = 1;
        String rootDir = ".";
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith(ID_ARG)) {
                try {
                    nodeId = Integer.valueOf(args[i].substring(ID_ARG.length()));
                } catch (NumberFormatException e) {
                    Log.println('E', LOG_TAG, "Invalid node id: " + args[i]);
                    System.exit(-1);
                }
            } else if (arg.startsWith(ROOT_DIR_ARG)) {
                rootDir = args[i].substring(ROOT_DIR_ARG.length());
            }
        }

        Looper.prepare();

        Environment.setRootDirectory(rootDir);

        File file = new File(Environment.getRootDirectory(), "res/MindroidRuntimeSystem.xml");
        Runtime.start(nodeId, file.exists() ? file : null);

        ServiceManager serviceManager = new ServiceManager();
        serviceManager.start();

        try {
            startSystemServices();
        } catch (Exception e) {
            throw new RuntimeException("System failure");
        }

        try {
            startServices();
        } catch (Exception e) {
            throw new RuntimeException("System failure");
        }

        Looper.loop();
    }

    public static void startSystemServices() throws InterruptedException, RemoteException {
        IServiceManager serviceManager = ServiceManager.getServiceManager();

        serviceManager.startSystemService(new Intent()
                .setComponent(Consts.LOGGER_SERVICE)
                .putExtra("name", Context.LOGGER_SERVICE.toString())
                .putExtra("process", "main"));

        serviceManager.startSystemService(new Intent(Logger.ACTION_LOG)
                .setComponent(Consts.LOGGER_SERVICE)
                .putExtra("logBuffer", Log.LOG_ID_MAIN)
                .putExtra("logPriority", Log.DEBUG)
                .putExtra("logFlags", new String[] { "timestamp" })
                .putExtra("consoleLogging", true)
                .putExtra("fileLogging", false)
                .putExtra("logFileName", "Log-%g.log")
                .putExtra("logFileLimit", 262144)
                .putExtra("logFileCount", 4));

        serviceManager.startSystemService(new Intent()
                .setComponent(Consts.PACKAGE_MANAGER)
                .putExtra("name", Context.PACKAGE_MANAGER.toString())
                .putExtra("process", "main"));
        ServiceManager.waitForSystemService(Context.PACKAGE_MANAGER);
    }

    private static void startServices() throws InterruptedException, RemoteException {
        final IPackageManager packageManager = IPackageManager.Stub.asInterface(ServiceManager.getSystemService(Context.PACKAGE_MANAGER));

        PackageManagerListener packageManagerListener = new PackageManagerListener() {
            public void onBootCompleted() {
                Log.i(LOG_TAG, "Boot completed");

                try {
                    IServiceManager serviceManager = ServiceManager.getServiceManager();

                    List packages = packageManager.getInstalledPackages(PackageManager.GET_SERVICES);
                    if (packages != null) {
                        for (Iterator itr = packages.iterator(); itr.hasNext();) {
                            PackageInfo p = (PackageInfo) itr.next();
                            if (p.services != null) {
                                ServiceInfo[] services = p.services;
                                for (int i = 0; i < services.length; i++) {
                                    ServiceInfo service = services[i];
                                    if (service.isEnabled() && service.hasFlag(ServiceInfo.FLAG_AUTO_START)) {
                                        Intent intent = new Intent();
                                        intent.setComponent(new ComponentName(service.packageName, service.name));
                                        serviceManager.startService(intent);
                                    }
                                }
                            }
                        }
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException("System failure");
                }
            }
        };

        packageManager.addListener(packageManagerListener.asInterface());
    }

    public static void shutdownServices() throws RemoteException, InterruptedException {
        IServiceManager serviceManager = ServiceManager.getServiceManager();
        IPackageManager packageManager = IPackageManager.Stub.asInterface(ServiceManager.getSystemService(Context.PACKAGE_MANAGER));

        try {
            List packages = packageManager.getInstalledPackages(PackageManager.GET_SERVICES);
            if (packages != null) {
                for (Iterator itr = packages.iterator(); itr.hasNext();) {
                    PackageInfo p = (PackageInfo) itr.next();
                    if (p.services != null) {
                        ServiceInfo[] services = p.services;
                        for (int i = 0; i < services.length; i++) {
                            ServiceInfo service = services[i];
                            if (service.isEnabled()) {
                                Intent intent = new Intent();
                                intent.setComponent(new ComponentName(service.packageName, service.name));
                                serviceManager.stopService(intent);
                            }
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            // Ignore exception.
        }
    }

    public static void shutdownSystemServices() throws RemoteException, InterruptedException {
        IServiceManager serviceManager = ServiceManager.getServiceManager();

        serviceManager.stopSystemService(new Intent().setComponent(Consts.PACKAGE_MANAGER));
        ServiceManager.waitForSystemServiceShutdown(Context.PACKAGE_MANAGER);

        serviceManager.stopSystemService(new Intent().setComponent(Consts.LOGGER_SERVICE));
        ServiceManager.waitForSystemServiceShutdown(Context.LOGGER_SERVICE);
    }
}
