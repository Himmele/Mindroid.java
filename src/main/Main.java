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
import mindroid.os.RemoteException;
import mindroid.os.ServiceManager;
import mindroid.util.Log;

public class Main {
	/**
	 * e.g. Linux: java -classpath Mindroid.jar:Main.jar main.Main rootDir=.
	 * e.g. Microsoft Windows: java -classpath Mindroid.jar;Main.jar main.Main rootDir=.
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
		} catch (RemoteException e) {
			System.exit(-1);
		} catch (InterruptedException e) {
			System.exit(-1);
		}

		Looper.prepare();
		final IPackageManager packageManager = IPackageManager.Stub.asInterface(ServiceManager.getSystemService(Context.PACKAGE_MANAGER));
		PackageManagerListener packageManagerListener = new PackageManagerListener() {
			public void onBootCompleted() {
				Log.i(LOG_TAG, "Boot completed");
				ComponentName[] services = null;
				try {
					services = packageManager.getAutostartServices();
				} catch (RemoteException e) {
				}
				IServiceManager serviceManager = ServiceManager.getServiceManager();
				
				for (int i = 0; i < services.length; i++) {
					Intent intent = new Intent();
					intent.setComponent(services[i]);
					try {
						serviceManager.startService(intent);
					} catch (RemoteException e) {
					}
				}
			}
		};
		try {
			packageManager.addListener(packageManagerListener.asBinder());
		} catch (RemoteException e) {
			System.exit(-1);
		}
		Looper.loop();
	}
	
	public static void startSystemServices() throws RemoteException, InterruptedException {
		IServiceManager serviceManager = ServiceManager.getServiceManager();

		ArrayList logBuffers = new ArrayList();
		logBuffers.add(new String("main"));
		serviceManager.startSystemService(new Intent()
				.setComponent(Consts.LOGGER_SERVICE)
				.putExtra("processName", "main")
				.putExtra("serviceName", Context.LOGGER_SERVICE)
				.putStringArrayListExtra("logBuffers", logBuffers)
				.putExtra("timestamps", false)
				.putExtra("priority", Log.INFO));

		serviceManager.startSystemService(new Intent()
				.setComponent(Consts.PACKAGE_MANAGER)
				.putExtra("processName", "main")
				.putExtra("serviceName", Context.PACKAGE_MANAGER));
		
		ServiceManager.waitForSystemService(Context.PACKAGE_MANAGER);
	}
	
	public static void shutdownApps() throws RemoteException, InterruptedException {
		IServiceManager serviceManager = ServiceManager.getServiceManager();
		IPackageManager packageManager = IPackageManager.Stub.asInterface(ServiceManager.getSystemService(Context.PACKAGE_MANAGER));
		if (packageManager != null) {
			ComponentName services[] = packageManager.getAutostartServices();
			for (int i = 0; i < services.length; i++) {
				serviceManager.stopService(new Intent().setComponent(services[i]));
			}
		}
	}
	
	public static void shutdownSystemServices() throws RemoteException, InterruptedException {
		IServiceManager serviceManager = ServiceManager.getServiceManager();

		serviceManager.stopSystemService(new Intent()
				.setComponent(Consts.PACKAGE_MANAGER));
		
		serviceManager.stopSystemService(new Intent()
			.setComponent(Consts.LOGGER_SERVICE));

		ServiceManager.waitForSystemServiceShutdown(Context.PACKAGE_MANAGER);
		ServiceManager.waitForSystemServiceShutdown(Context.LOGGER_SERVICE);
	}
}
