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

package mindroid.testing;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import mindroid.content.ComponentName;
import mindroid.content.Context;
import mindroid.content.Intent;
import mindroid.content.pm.PackageInfo;
import mindroid.content.pm.PackageInstaller;
import mindroid.content.pm.PackageManager;
import mindroid.content.pm.ServiceInfo;
import mindroid.os.Environment;
import mindroid.os.IServiceManager;
import mindroid.os.RemoteException;
import mindroid.os.ServiceManager;
import mindroid.util.Log;
import mindroid.util.concurrent.CancellationException;
import mindroid.util.concurrent.ExecutionException;
import mindroid.util.concurrent.TimeoutException;
import mindroid.util.logging.Logger;
import mindroid.runtime.system.Runtime;

public class IntegrationTest {
    private static final String LOG_TAG = "Mindroid";
    private static final ComponentName SERVICE_MANAGER = new ComponentName("mindroid.os", "ServiceManager");
    private static final ComponentName PACKAGE_MANAGER = new ComponentName("mindroid.content.pm", "PackageManagerService");
    private static final ComponentName LOGGER_SERVICE = new ComponentName("mindroid.util.logging", "Logger");
    private static final ComponentName CONSOLE_SERVICE = new ComponentName("mindroid.testing.console", "ConsoleService");

    private static ServiceManager sServiceManager;

    public static void setUp() {
        final int nodeId = 1;
        final String rootDir = ".";

        Environment.setRootDirectory(rootDir);

        File file = new File(Environment.getRootDirectory(), "res/MindroidRuntimeSystem.xml");
        Runtime.start(nodeId, file.exists() ? file : null);

        sServiceManager = new ServiceManager();
        sServiceManager.start();

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
    }

    public static void tearDown() {
        try {
            shutdownServices();
        } catch (Exception e) {
            throw new RuntimeException("System failure");
        }

        try {
            shutdownSystemServices();
        } catch (Exception e) {
            throw new RuntimeException("System failure");
        }

        sServiceManager.shutdown();
        sServiceManager = null;

        Runtime.shutdown();
    }

    public static void startSystemServices() throws InterruptedException, RemoteException {
        IServiceManager serviceManager = ServiceManager.getServiceManager();

        serviceManager.startSystemService(new Intent()
                .setComponent(LOGGER_SERVICE)
                .putExtra("name", Context.LOGGER_SERVICE.toString())
                .putExtra("process", "main"));

        serviceManager.startSystemService(new Intent(Logger.ACTION_LOG)
                .setComponent(LOGGER_SERVICE)
                .putExtra("logBuffer", Log.LOG_ID_MAIN)
                .putExtra("logPriority", Log.DEBUG)
                .putExtra("logFlags", new String[] { "timestamp" })
                .putExtra("consoleLogging", true)
                .putExtra("fileLogging", false)
                .putExtra("logFileName", "Log-%g.log")
                .putExtra("logFileLimit", 262144)
                .putExtra("logFileCount", 4));

        serviceManager.startSystemService(new Intent()
                .setComponent(CONSOLE_SERVICE)
                .putExtra("name", Context.CONSOLE_SERVICE.toString())
                .putExtra("process", "main"));
        ServiceManager.waitForSystemService(Context.CONSOLE_SERVICE);

        serviceManager.startSystemService(new Intent()
                .setComponent(PACKAGE_MANAGER)
                .putExtra("name", Context.PACKAGE_MANAGER.toString())
                .putExtra("process", "main"));
        ServiceManager.waitForSystemService(Context.PACKAGE_MANAGER);
    }

    private static void startServices() throws InterruptedException, RemoteException {
        PackageManager packageManager = new PackageManager();
        PackageInstaller packageInstaller = new PackageInstaller();
        packageInstaller.install(Environment.getAppsDirectory());
        List packages = packageManager.getInstalledPackages(PackageManager.GET_SERVICES);
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
                                serviceManager.startService(service).get(10000);
                            } catch (CancellationException | ExecutionException | TimeoutException | RemoteException e) {
                                throw new RuntimeException("System failure");
                            }
                        }
                    }
                }
            }
        }
    }

    public static void shutdownServices() throws RemoteException, InterruptedException {
        PackageManager packageManager = new PackageManager();
        List packages = packageManager.getInstalledPackages(PackageManager.GET_SERVICES);
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
                                serviceManager.stopService(service).get(10000);
                            } catch (CancellationException | ExecutionException | TimeoutException | RemoteException ignore) {
                            }
                        }
                    }
                }
            }
        }
    }

    public static void shutdownSystemServices() throws RemoteException, InterruptedException {
        IServiceManager serviceManager = ServiceManager.getServiceManager();

        serviceManager.stopSystemService(new Intent().setComponent(PACKAGE_MANAGER));
        ServiceManager.waitForSystemServiceShutdown(Context.PACKAGE_MANAGER);

        serviceManager.stopSystemService(new Intent().setComponent(CONSOLE_SERVICE));
        ServiceManager.waitForSystemServiceShutdown(Context.CONSOLE_SERVICE);

        serviceManager.stopSystemService(new Intent().setComponent(LOGGER_SERVICE));
        ServiceManager.waitForSystemServiceShutdown(Context.LOGGER_SERVICE);
    }
}
