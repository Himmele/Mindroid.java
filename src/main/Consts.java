package main;

import mindroid.content.ComponentName;

public class Consts {
	public static final ComponentName SERVICE_MANAGER = new ComponentName("mindroid.os", "ServiceManager");
	public static final ComponentName PACKAGE_MANAGER = new ComponentName("mindroid.content.pm", "PackageManagerService");
	public static final ComponentName SYSTEM_LOGGER_SERVICE = new ComponentName("mindroid.util", "Logger");
	public static final ComponentName LOCATION_MANAGER_SERVICE = new ComponentName("mindroid.location", "LocationManagerService");
	public static final ComponentName TELEPHONY_SERVICE = new ComponentName("mindroid.telephony", "TelephonyManagerService");
}
