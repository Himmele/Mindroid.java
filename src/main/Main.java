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
import mindroid.content.ComponentName;
import mindroid.content.Context;
import mindroid.content.Intent;
import mindroid.content.pm.PackageManager;
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

    private static final ComponentName SERVICE_MANAGER = new ComponentName("mindroid.os", "ServiceManager");
    private static final ComponentName PACKAGE_MANAGER = new ComponentName("mindroid.content.pm", "PackageManagerService");
    private static final ComponentName LOGGER_SERVICE = new ComponentName("mindroid.util.logging", "LoggerService");
    private static final ComponentName CONSOLE_SERVICE = new ComponentName("mindroid.runtime.inspection", "ConsoleService");

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
                    nodeId = Integer.valueOf(arg.substring(ID_ARG.length()));
                } catch (NumberFormatException e) {
                    Log.println('E', LOG_TAG, "Invalid node id: " + arg);
                    System.exit(-1);
                }
            } else if (arg.startsWith(ROOT_DIR_ARG)) {
                rootDir = arg.substring(ROOT_DIR_ARG.length());
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
                .setComponent(LOGGER_SERVICE)
                .putExtra("name", Context.LOGGER_SERVICE.toString())
                .putExtra("process", "main"));
        ServiceManager.waitForSystemService(Context.LOGGER_SERVICE);

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
        IServiceManager serviceManager = ServiceManager.getServiceManager();
        serviceManager.startSystemService(new Intent(PackageManager.ACTION_START_APPLICATIONS)
                .setComponent(PACKAGE_MANAGER));
    }

    public static void shutdownServices() throws RemoteException, InterruptedException {
        IServiceManager serviceManager = ServiceManager.getServiceManager();
        serviceManager.startSystemService(new Intent(PackageManager.ACTION_SHUTDOWN_APPLICATIONS)
                .setComponent(PACKAGE_MANAGER));
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
