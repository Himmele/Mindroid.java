package main;

import java.util.ArrayList;
import mindroid.content.ComponentName;
import mindroid.content.Context;
import mindroid.content.Intent;
import mindroid.content.pm.IPackageManager;
import mindroid.content.pm.PackageManagerListener;
import mindroid.os.Environment;
import mindroid.os.IServiceManager;
import mindroid.os.Looper;
import mindroid.os.ServiceManager;
import mindroid.util.Log;

public class Main {
	/**
	 * e.g. Linux: java -classpath Mindroid.jar:Main.jar main.Main rootDir=../../
	 * e.g. Microsoft Windows: java -classpath Mindroid.jar;Main.jar main.Main rootDir=../../
	 */
	public static void main(String[] args) {
		final String LOG_TAG = "main";
		String rootDir = ".";
		if (args.length > 0) {
			if (args[0].startsWith("rootDir=")) {
				rootDir = args[0].substring("rootDir=".length());
			}
		}
		Environment.setRootDirectory(rootDir);
		
		ServiceManager serviceManager = new ServiceManager();
		serviceManager.start();

		try {
			startSystemServices();
		} catch (InterruptedException e) {
			// TODO: Restart.
		}

		Looper.prepare();
		final IPackageManager packageManager = IPackageManager.Stub.asInterface(ServiceManager.getSystemService(Context.PACKAGE_MANAGER));
		PackageManagerListener packageManagerListener = new PackageManagerListener() {
			public void onBootCompleted() {
				Log.i(LOG_TAG, "Boot completed");
				ComponentName[] services = packageManager.getAutostartServices();
				IServiceManager serviceManager = ServiceManager.getIServiceManager();
				
				for (int i = 0; i < services.length; i++) {
					Intent intent = new Intent();
					intent.setComponent(services[i]);
					serviceManager.startService(intent);
				}
			}
		};
		packageManager.addListener(packageManagerListener.asBinder());
		Looper.loop();
	}
	
	public static void startSystemServices() throws InterruptedException {
		IServiceManager serviceManager = ServiceManager.getIServiceManager();

		ArrayList logIds = new ArrayList();
		logIds.add(new Integer(Log.LOG_ID_MAIN));
		serviceManager.startSystemService(new Intent()
				.setComponent(Consts.CONSOLE_LOGGER_SERVICE)
				.putExtra("processName", "main")
				.putExtra("serviceName", Context.CONSOLE_LOGGER_SERVICE)
				.putExtra("timestamps", false)
				.putExtra("priority", Log.INFO)
				.putIntegerArrayListExtra("logIds", logIds));

		serviceManager.startSystemService(new Intent()
				.setComponent(Consts.PACKAGE_MANAGER)
				.putExtra("processName", "main")
				.putExtra("serviceName", Context.PACKAGE_MANAGER));
		
		ServiceManager.waitForSystemService(Context.PACKAGE_MANAGER);
	}
	
	public static void shutdownApps() throws InterruptedException {
		IServiceManager serviceManager = ServiceManager.getIServiceManager();
		IPackageManager packageManager = IPackageManager.Stub.asInterface(ServiceManager.getSystemService(Context.PACKAGE_MANAGER));
		if (packageManager != null) {
			ComponentName services[] = packageManager.getAutostartServices();
			for (int i = 0; i < services.length; i++) {
				serviceManager.stopService(new Intent().setComponent(services[i]));
			}
		}
	}
	
	public static void shutdownSystemServices() throws InterruptedException {
		IServiceManager serviceManager = ServiceManager.getIServiceManager();

		serviceManager.stopSystemService(new Intent()
				.setComponent(Consts.PACKAGE_MANAGER));
		
		serviceManager.stopSystemService(new Intent()
			.setComponent(Consts.CONSOLE_LOGGER_SERVICE));

		ServiceManager.waitForSystemServiceShutdown(Context.PACKAGE_MANAGER);
		ServiceManager.waitForSystemServiceShutdown(Context.CONSOLE_LOGGER_SERVICE);
	}
}
