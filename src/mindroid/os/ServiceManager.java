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

package mindroid.os;

import java.util.Collection;
import java.util.HashMap;
import mindroid.app.Process;
import mindroid.content.ComponentName;
import mindroid.content.Context;
import mindroid.content.Intent;
import mindroid.content.ServiceConnection;
import mindroid.content.pm.ApplicationInfo;
import mindroid.content.pm.IPackageManager;
import mindroid.content.pm.ServiceInfo;
import mindroid.util.Log;

public final class ServiceManager {
	private static final String LOG_TAG = "ServiceManager";
	private static final String SYSTEM_SERVICE = "systemService";
	private static IServiceManager sServiceManager;
	private static IServiceManager.Stub sMessenger = null;
	private static HashMap sSystemServices = new HashMap();
	private static final int PREPARE_SERVICE_DONE = 1;
	private static final int BIND_SERVICE_DONE = 2;
	private static final int SHUTDOWN_TIMEOUT = 10000; //ms
	private final HandlerThread mMainThread;
	private MainHandler mMainHandler;
	private IPackageManager mPackageManager = null;
	private HashMap mProcesses = new HashMap();
	private HashMap mServices = new HashMap();
	private int mStartId = 0;
	
	private Process.UncaughtExceptionHandler mUncaughtExceptionHandler = new Process.UncaughtExceptionHandler() {
		public void uncaughtException(Process process, Throwable e) {
			ProcessRecord processRecord;
			synchronized (mProcesses) {
				processRecord = (ProcessRecord) mProcesses.get(process.getName());
			}
			String message = e.getMessage();
			if (message == null) {
				message = e.toString();
			}
			System.out.println("Uncaught exception in process " + processRecord.processName + ": " + message);
		}
	};
	
	static class ProcessRecord {
		final String processName;
		final Process process;
		private final HashMap services = new HashMap();
		
		public ProcessRecord(String processName, Process process) {
			this.processName = processName;
			this.process = process;
		}
		
		void addService(ComponentName component, ServiceRecord service) {
			services.put(component, service);
		}
		
		void removeService(ComponentName component) {
			services.remove(component);
		}
		
		ServiceRecord getService(ComponentName component) {
			return (ServiceRecord) services.get(component);
		}
		
		boolean containsService(ComponentName component) {
			return services.containsKey(component);
		}
		
		int numServices() {
			return services.size();
		}
	}
	
	static class ServiceRecord {
		final ComponentName component;
		final String serviceName;
		final ProcessRecord process;
		final boolean systemService;
		boolean alive;
		boolean running;
		private final HashMap serviceConnections = new HashMap();
		
		ServiceRecord(ComponentName component, ProcessRecord process) {
			this.component = component;
			this.process = process;
			this.serviceName = null;
			systemService = false;
			alive = false;
			running = false;
		}
		
		ServiceRecord(ComponentName component, String serviceName, ProcessRecord process, boolean systemService) {
			this.component = component;
			this.process = process;
			this.serviceName = serviceName;
			this.systemService = systemService;
			alive = false;
			running = false;
		}
		
		void addServiceConnection(ServiceConnection conn, ComponentName target) {
			serviceConnections.put(conn, target);
		}
		
		void removeServiceConnection(ServiceConnection conn) {
			serviceConnections.remove(conn);
		}
		
		int getNumServiceConnections() {
			return serviceConnections.size();
		}
		
		boolean hasServiceConnection(ServiceConnection conn) {
			return serviceConnections.containsKey(conn);
		}
	}
	
	public ServiceManager() {
		mMainThread = new HandlerThread(LOG_TAG);
	}
	
	public void start() {
		mMainThread.start();
		mMainHandler = new MainHandler(mMainThread.getLooper());
		sMessenger = new Messenger(mMainThread.getLooper());
		
		addService(Context.SERVICE_MANAGER, sMessenger);
	}
	
	public void shutdown() {
		removeService(Context.SERVICE_MANAGER);
		
		ProcessRecord processRecords[];
		synchronized (mProcesses) {
			Collection prs = mProcesses.values();
			processRecords = new ProcessRecord[prs.size()];
			prs.toArray(processRecords);
		}
		for (int i = 0; i < processRecords.length; i++) {
			ProcessRecord processRecord = processRecords[i];
			processRecord.process.stop();
		}
		
		mMainThread.quit();
		try {
			mMainThread.join(SHUTDOWN_TIMEOUT);
		} catch (InterruptedException e) {
		}
		
		sServiceManager = null;
	}
	
	private class MainHandler extends Handler {
		public MainHandler(Looper looper) {
			super(looper);
		}
		
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PREPARE_SERVICE_DONE: {
				ComponentName component = (ComponentName) msg.obj;
				if (mServices.containsKey(component)) {
					ServiceRecord serviceRecord = (ServiceRecord) mServices.get(component);
					if (msg.arg1 == 0) {
						Log.d(LOG_TAG, "Service " + serviceRecord.serviceName + " has been created in process " + serviceRecord.process.processName);
					} else {
						Intent intent = new Intent();
						intent.setComponent(component);
						cleanupService(intent);
						Log.d(LOG_TAG, "Cannot create service " + serviceRecord.serviceName + ". Cleaning up");
					}
				}
				break;
			}
			case BIND_SERVICE_DONE: {
				ComponentName caller = (ComponentName) msg.getData().getObject("caller");
				final ComponentName service = (ComponentName) msg.getData().getObject("service");
				final ServiceConnection conn = (ServiceConnection) msg.getData().getObject("conn");
				final IBinder binder = (IBinder) msg.getData().getObject("binder");
				
				ServiceRecord serviceRecord = (ServiceRecord) mServices.get(service);
				if (serviceRecord != null) {
					Process callerProcess = getProcessForComponent(caller);
					if (callerProcess != null) {
						callerProcess.runOnMainThread(new Runnable() {
							public void run() {
								conn.onServiceConnected(service, binder);
							}
						});
					}
				}
				break;
			}
			default:
				super.handleMessage(msg);
			}
		}
	}
	
	class Messenger extends IServiceManager.Stub {
		public Messenger(Looper looper) {
			super(looper);
		}
		
		public ComponentName startService(Intent service) {
			service.putExtra(SYSTEM_SERVICE, false);
			if (mPackageManager == null) {
				mPackageManager = IPackageManager.Stub.asInterface(getSystemService(Context.PACKAGE_MANAGER));
			}
			ServiceInfo serviceInfo = null;
			try {
				serviceInfo = mPackageManager.resolveService(service);
			} catch (RemoteException e) {
				// Ignore exception
			}
			if (serviceInfo == null) {
				return null;
			}
			return ServiceManager.this.startService(service, serviceInfo);
		}
		
		public boolean stopService(Intent service) {
			if (mServices.containsKey(service.getComponent())) {
				final ServiceRecord serviceRecord = (ServiceRecord) mServices.get(service.getComponent());
				final ProcessRecord processRecord = serviceRecord.process;
				final Process process = processRecord.process;
				
				if (!serviceRecord.alive) {
					return false;
				}
				if (serviceRecord.running) {
					serviceRecord.running = false;
				}
				
				if (serviceRecord.getNumServiceConnections() != 0) {
					Log.d(LOG_TAG, "Cannot stop service " + service.getComponent().toShortString() + " because of active bindings");
					return false;
				}
				
				Message reply = Message.obtain(mMainHandler, new Runnable() {
					public void run() {
						Log.d(LOG_TAG, "Service " + serviceRecord.serviceName + " has been stopped");
					}
				});
				serviceRecord.alive = false;
				process.stopService(service, reply);
				
				mServices.remove(service.getComponent());
				processRecord.removeService(service.getComponent());
				
				if (processRecord.numServices() == 0) {
					mMainHandler.post(new Runnable() {
						public void run() {
							if (processRecord.numServices() == 0) {
								process.stop();
								synchronized (mProcesses) {
									mProcesses.remove(processRecord.processName);
								}
							}
						}
					});
				}
				
				return true;
			} else {
				Log.d(LOG_TAG, "Cannot find and stop service " + service.getComponent().toShortString());
				return false;
			}
		}
		
		public ComponentName startService(ComponentName caller, Intent service) {
			return startService(service);
		}
		
		public boolean stopService(ComponentName caller, Intent service) {
			return stopService(service);
		}

		public boolean bindService(ComponentName caller, Intent service, ServiceConnection conn, int flags) {
			if (caller == null) {
				return false;
			}
			service.putExtra(SYSTEM_SERVICE, false);
			if (mPackageManager == null) {
				mPackageManager = IPackageManager.Stub.asInterface(getSystemService(Context.PACKAGE_MANAGER));
			}
			ServiceInfo serviceInfo = null;
			try {
				serviceInfo = mPackageManager.resolveService(service);
			} catch (RemoteException e) {
				// Ignore exception
			}
			if (serviceInfo == null) {
				return false;
			}
			Process process = prepareService(service, serviceInfo);
			if (process == null) {
				return false;
			}
			
			if (mServices.containsKey(service.getComponent())) {
				ServiceRecord serviceRecord = (ServiceRecord) mServices.get(service.getComponent());
				if (!serviceRecord.hasServiceConnection(conn)) {
					serviceRecord.addServiceConnection(conn, caller);
					
					Message reply = mMainHandler.obtainMessage(BIND_SERVICE_DONE);
					Bundle data = new Bundle();
					data.putObject("caller", caller);
					data.putObject("service", service.getComponent());
					data.putObject("conn", conn);
					reply.setData(data);
					process.bindService(service, conn, flags, reply);
					
					ProcessRecord processRecord = serviceRecord.process;
					Log.d(LOG_TAG, "Bound to service " + serviceRecord.serviceName + " in process " + processRecord.processName);
				}

				return true;
			} else {
				Log.d(LOG_TAG, "Cannot find and bind service " + service.getComponent().toShortString());
				return false;
			}
		}

		public void unbindService(ComponentName caller, Intent service, ServiceConnection conn) {
			if (caller == null) {
				return;
			}
			
			ServiceRecord serviceRecord = (ServiceRecord) mServices.get(service.getComponent());
			if (serviceRecord != null) {
				ProcessRecord processRecord = serviceRecord.process;
				Process process = processRecord.process;
				serviceRecord.removeServiceConnection(conn);
				
				Message reply = Message.obtain(mMainHandler, new Runnable() {
					public void run() {
					}
				});
				process.unbindService(service, reply);
				Log.d(LOG_TAG, "Unbound from service " + serviceRecord.serviceName + " in process " + processRecord.processName);
				
				if (serviceRecord.getNumServiceConnections() == 0) {
					sServiceManager.stopService(service);
				}
			}
		}

		public ComponentName startSystemService(Intent service) {
			final String processName = service.getStringExtra("processName");
			final String serviceName = service.getStringExtra("serviceName");
			service.putExtra(SYSTEM_SERVICE, true);
			
			if (processName == null || serviceName == null) {
				return null;
			}
			
			ApplicationInfo applicationInfo = new ApplicationInfo();
			applicationInfo.processName = processName;
			ServiceInfo serviceInfo = new ServiceInfo();
			serviceInfo.processName = processName;
			serviceInfo.serviceName = serviceName;
			serviceInfo.applicationInfo = applicationInfo;
			serviceInfo.systemService = true;
			
			return ServiceManager.this.startService(service, serviceInfo);
		}

		public boolean stopSystemService(Intent service) {
			return stopService(service);
		}
	}
	
	private Process prepareService(Intent service, final ServiceInfo serviceInfo) {
		if (!serviceInfo.applicationInfo.enabled || !serviceInfo.enabled) {
			return null;
		}
		
		final ProcessRecord processRecord;
		final Process process;
		synchronized (mProcesses) {
			if (mProcesses.containsKey(serviceInfo.processName)) {
				processRecord = (ProcessRecord) mProcesses.get(serviceInfo.processName);
				process = processRecord.process;
			} else {
				process = new Process(serviceInfo.processName);
				processRecord = new ProcessRecord(serviceInfo.processName, process);
				mProcesses.put(serviceInfo.processName, processRecord);
			}
		}
		if (!process.isAlive()) {
			process.setUncaughtExceptionHandler(mUncaughtExceptionHandler);
			process.start();
		}
		
		final ServiceRecord serviceRecord;
		if (mServices.containsKey(service.getComponent())) {
			serviceRecord = (ServiceRecord) mServices.get(service.getComponent());
		} else {
			boolean systemService = service.getBooleanExtra(SYSTEM_SERVICE, false);
			serviceRecord = new ServiceRecord(service.getComponent(), serviceInfo.serviceName, processRecord, systemService);
			mServices.put(service.getComponent(), serviceRecord);
		}
		
		if (!processRecord.containsService(service.getComponent())) {
			processRecord.addService(service.getComponent(), serviceRecord);
		}
		
		if (!serviceRecord.alive) {
			serviceRecord.alive = true;
			Message reply = mMainHandler.obtainMessage(PREPARE_SERVICE_DONE, service.getComponent());
			process.createService(service, serviceInfo, reply);
		}
		return process;
	}
	
	private ComponentName startService(Intent service, final ServiceInfo serviceInfo) {
		Process process = prepareService(service, serviceInfo);
		if (process == null) {
			return null;
		}
		
		Message reply = Message.obtain(mMainHandler, new Runnable() {
			public void run() {
				Log.d(LOG_TAG, "Service " + serviceInfo.serviceName + " has been started in process " + serviceInfo.processName);
			}
		});
		ServiceRecord serviceRecord = (ServiceRecord) mServices.get(service.getComponent());
		serviceRecord.running = true;
		process.startService(service, 0, mStartId++, reply);
		
		return service.getComponent();
	}
	
	private boolean cleanupService(Intent service) {
		if (mServices.containsKey(service.getComponent())) {
			final ServiceRecord serviceRecord = (ServiceRecord) mServices.get(service.getComponent());
			final ProcessRecord processRecord = serviceRecord.process;
			final Process process = processRecord.process;
			
			if (serviceRecord.alive) {
				serviceRecord.alive = false;
			}
			if (serviceRecord.running) {
				serviceRecord.running = false;
			}
			
			mServices.remove(service.getComponent());
			processRecord.removeService(service.getComponent());
			
			if (processRecord.numServices() == 0) {
				mMainHandler.post(new Runnable() {
					public void run() {
						if (processRecord.numServices() == 0) {
							process.stop();
							synchronized (mProcesses) {
								mProcesses.remove(processRecord.processName);
							}
						}
					}
				});
			}
			
			return true;
		} else {
			Log.d(LOG_TAG, "Cannot find and clean up service " + service.getComponent().toShortString());
			return false;
		}
	}
	
	private Process getProcessForComponent(ComponentName component) {
		ServiceRecord serviceRecord = (ServiceRecord) mServices.get(component);
		if (serviceRecord != null) {
			ProcessRecord processRecord = serviceRecord.process;
			return processRecord.process;
		} else {
			return null;
		}
	}
	
	public static synchronized IServiceManager getIServiceManager() {
        if (sServiceManager != null) {
            return sServiceManager;
        }

        sServiceManager = IServiceManager.Stub.asInterface(sMessenger);
        return sServiceManager;
    }
	
	/**
     * Returns a reference to a service with the given name.
     * 
     * @param name the name of the service to get
     * @return a reference to the service, or <code>null</code> if the service doesn't exist
     */
    public static IBinder getSystemService(String name) {
    	synchronized (sSystemServices) {
	    	IBinder service = (IBinder) sSystemServices.get(name);
	        return service;
    	}
    }
    
    /**
     * @hide
     */
    public static void addService(String name, IBinder service) {
    	synchronized (sSystemServices) {
    		if (!sSystemServices.containsKey(name)) {
	    		sSystemServices.put(name, service);
	    		sSystemServices.notifyAll();
    		}
    	}
    }
    
    /**
     * @hide
     */
    public static void removeService(String name) {
    	synchronized (sSystemServices) {
    		sSystemServices.remove(name);
    		sSystemServices.notifyAll();
    	}
    }
    
    /**
     * @hide
     */
    public static void waitForSystemService(String name) throws InterruptedException {
    	synchronized (sSystemServices) {
    		final long TIMEOUT = 4000;
    		long timeout = TIMEOUT;
    		long refTime = System.currentTimeMillis();
    		while (!sSystemServices.containsKey(name)) {
    			sSystemServices.wait(timeout);
    			long curTime = System.currentTimeMillis();
    			if (curTime - refTime >= TIMEOUT) {
	    			if (!sSystemServices.containsKey(name)) {
	    				Log.w(LOG_TAG, "Starting " + name + " takes very long");
	    				timeout = TIMEOUT;
	    				refTime = System.currentTimeMillis();
	    			} else {
	    				break;
	    			}
    			} else {
    				timeout = TIMEOUT - (curTime - refTime);
    			}
    		}
    	}
    }
    
    /**
     * @hide
     */
    public static void waitForSystemServiceShutdown(String name) throws InterruptedException {
    	synchronized (sSystemServices) {
    		final long TIMEOUT = 10000;
    		long timeout = TIMEOUT;
    		long refTime = System.currentTimeMillis();
    		int numAttempts = 10;
    		while (sSystemServices.containsKey(name)) {
    			sSystemServices.wait(timeout);
    			long curTime = System.currentTimeMillis();
    			if (curTime - refTime >= TIMEOUT) {
	    			if (sSystemServices.containsKey(name)) {
	    				Log.w(LOG_TAG, "Stopping " + name + " takes very long");
	    				timeout = TIMEOUT;
	    				refTime = System.currentTimeMillis();
	    				numAttempts--;
	    				if (numAttempts <= 0) {
	    					break;
	    				}
	    			} else {
	    				break;
	    			}
    			} else {
    				timeout = TIMEOUT - (curTime - refTime);
    			}
    		}
    	}
    }
}
