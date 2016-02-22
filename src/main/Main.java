package main;

import java.util.ArrayList;
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

public class Main {
	private static final String LOG_TAG = "Mindroid";

	/**
	 * Linux: java -classpath Mindroid.jar:Main.jar main.Main rootDir=.
	 * Microsoft Windows: java -classpath Mindroid.jar;Main.jar main.Main rootDir=.
	 */
	public static void main(String[] args) {
		Looper.prepare();

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

		ArrayList logBuffers = new ArrayList();
		logBuffers.add(new String("main"));
		serviceManager.startSystemService(new Intent()
				.setComponent(Consts.LOGGER_SERVICE)
				.putExtra("name", Context.LOGGER_SERVICE)
				.putExtra("process", "main")
				.putStringArrayListExtra("logBuffers", logBuffers).putExtra("timestamps", false).putExtra("priority", Log.INFO));

		serviceManager.startSystemService(new Intent()
				.setComponent(Consts.PACKAGE_MANAGER)
				.putExtra("name", Context.PACKAGE_MANAGER)
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
