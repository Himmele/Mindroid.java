/*
 * Copyright (C) 2012 Daniel Himmelein
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

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import mindroid.content.ComponentName;
import mindroid.content.Context;
import mindroid.content.Intent;
import mindroid.content.ServiceConnection;
import mindroid.content.pm.IPackageManager;
import mindroid.content.pm.ResolveInfo;
import mindroid.content.pm.ServiceInfo;
import mindroid.app.ContextImpl;
import mindroid.app.Service;
import mindroid.util.Log;
import mindroid.util.concurrent.CancellationException;
import mindroid.util.concurrent.ExecutionException;
import mindroid.util.concurrent.SettableFuture;

public class Process {
	private static final String LOG_TAG = "Process";
	private final String mName;
	private final ThreadGroup mThreadGroup;
	private final HandlerThread mMainThread;
	private Handler mMainHandler;
	private IProcess.Stub mStub;
	private IPackageManager mPackageManager;
	private final HashMap mServices;
	private final Debug mDebug;
	private boolean mUncaughtException = false;

	public Process(String name) {
		mName = name;
		mThreadGroup = new ThreadGroup("Process {" + name + "}") {
			public void uncaughtException(Thread thread, Throwable e) {
				String message = e.getMessage();
				if (message == null) {
					message = e.toString();
				}
				System.out.println("Uncaught exception in process " + mName + ": " + message);
				mUncaughtException = true;
				synchronized (mServices) {
					mServices.notifyAll();
				}
				super.uncaughtException(thread, e);
			}
		};
		mMainThread = new HandlerThread(mThreadGroup, "Process {" + name + "}");
		mDebug = Debug.Creator.createInstance();
		mServices = new HashMap();
	}

	public IProcess start() {
		Log.d(LOG_TAG, "Starting process " + mName);
		mMainThread.start();
		mMainHandler = new Handler(mMainThread.getLooper());
		final SettableFuture future = new SettableFuture();
		mMainHandler.post(new Runnable() {
			public void run() {
				future.set(new ProcessImpl());
			}
		});
		mDebug.start(this);

		try {
			mStub = (IProcess.Stub) future.get();
		} catch (CancellationException e) {
			throw new RuntimeException("System failure");
		} catch (ExecutionException e) {
			throw new RuntimeException("System failure");
		} catch (InterruptedException e) {
			throw new RuntimeException("System failure");
		}
		return IProcess.Stub.asInterface(mStub);
	}

	public void stop(long timeout) {
		Log.d(LOG_TAG, "Stopping process " + mName);

		mDebug.stop();

		synchronized (mServices) {
			if (!mServices.isEmpty()) {
				IProcess process = IProcess.Stub.asInterface(mStub);

				Iterator itr = mServices.entrySet().iterator();
				while (itr.hasNext()) {
					Map.Entry pair = (Map.Entry) itr.next();
					ComponentName component = (ComponentName) pair.getKey();
					Intent intent = new Intent();
					intent.setComponent(component);

					try {
						process.stopService(intent);
					} catch (RemoteException e) {
						throw new RuntimeException("System failure");
					}
				}

				long start = SystemClock.uptimeMillis();
				long duration = timeout;
				while (!mUncaughtException && !mServices.isEmpty() && (duration > 0)) {
					try {
						mServices.wait(duration);
					} catch (InterruptedException e) {
					}
					duration = start + timeout - SystemClock.uptimeMillis();
				}
			}
		}

		mMainThread.quit();
		try {
			mMainThread.join();
		} catch (InterruptedException e) {
		}

		Log.d(LOG_TAG, "Process " + mName + " has been stopped");
	}

	public String getName() {
		return mName;
	}

	public ThreadGroup getThreadGroup() {
		return mThreadGroup;
	}

	public HandlerThread getMainThread() {
		return mMainThread;
	}

	public boolean isAlive() {
		return mMainThread.isAlive();
	}

	class ProcessImpl extends IProcess.Stub {
		public void createService(Intent intent, IRemoteCallback callback) throws RemoteException {
			Service service = null;
			String componentName = null;
			Bundle result = new Bundle();

			try {
				componentName = intent.getComponent().getPackageName() + "." + intent.getComponent().getClassName();
				if (intent.getBooleanExtra("systemService", false)) {
					try {
						Class clazz = Class.forName(componentName);
						service = (Service) clazz.newInstance();
					} catch (Exception e) {
						Log.e(LOG_TAG, "Cannot find class \'" + componentName + "\': " + e.getMessage(), e);
						throw e;
					} catch (LinkageError e) {
						Log.e(LOG_TAG, "Linkage error: " + e.getMessage(), e);
						throw e;
					}
				} else {
					if (mPackageManager == null) {
						mPackageManager = IPackageManager.Stub.asInterface(ServiceManager.getSystemService(Context.PACKAGE_MANAGER));
					}
					ResolveInfo resolveInfo = null;
					ServiceInfo serviceInfo = null;
					try {
						resolveInfo = mPackageManager.resolveService(intent, 0);
					} catch (NullPointerException e) {
						throw new RuntimeException("System failure");
					} catch (RemoteException e) {
						throw new RuntimeException("System failure");
					}
					if (resolveInfo != null && resolveInfo.serviceInfo != null) {
						serviceInfo = resolveInfo.serviceInfo;
					} else {
						throw new Exception("Unknown service " + intent.getComponent().toShortString());
					}

					if (!serviceInfo.isEnabled()) {
						throw new Exception("Service not enabled " + intent.getComponent().toShortString());
					}

					List urls = new ArrayList();
					if (serviceInfo.applicationInfo.libraries != null) {
						for (int i = 0; i < serviceInfo.applicationInfo.libraries.length; i++) {
							urls.add(new File(serviceInfo.applicationInfo.libraries[i]).toURI().toURL());
						}
					}
					urls.add(new File(serviceInfo.applicationInfo.fileName).toURI().toURL());
					URLClassLoader classLoader = new URLClassLoader((URL[]) urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
					try {
						Class clazz = Class.forName(componentName, true, classLoader);
						service = (Service) clazz.newInstance();
					} catch (Exception e) {
						Log.e(LOG_TAG, "Cannot find class \'" + componentName + "\': " + e.getMessage(), e);
						throw e;
					} catch (LinkageError e) {
						Log.e(LOG_TAG, "Linkage error: " + e.getMessage(), e);
						throw e;
					}
				}

				service.attach(new ContextImpl(mMainThread, intent.getComponent()), this, intent.getComponent().getClassName());
				synchronized (mServices) {
					mServices.put(intent.getComponent(), service);
				}
				service.onCreate();
				result.putBoolean("result", true);
			} catch (RuntimeException e) {
				Log.d(LOG_TAG, "Cannot create service " + componentName + ": " + e, e);
				throw e;
			} catch (Exception e) {
				Log.d(LOG_TAG, "Cannot create service " + componentName + ": " + e, e);
				result.putBoolean("result", false);
			} catch (Error e) {
				Log.d(LOG_TAG, "Cannot create service " + componentName + ": " + e, e);
				result.putBoolean("result", false);
			}

			callback.sendResult(result);
		}

		public void startService(Intent intent, int flags, int startId, IRemoteCallback callback) throws RemoteException {
			Service service;
			Bundle result = new Bundle();

			synchronized (mServices) {
				service = (Service) mServices.get(intent.getComponent());
			}
			if (service != null) {
				service.onStartCommand(intent, flags, startId);
				result.putBoolean("result", true);
			} else {
				result.putBoolean("result", false);
			}

			callback.sendResult(result);
		}

		public void stopService(Intent intent) throws RemoteException {
			stopService(intent, null);
		}

		public void stopService(Intent intent, IRemoteCallback callback) throws RemoteException {
			Service service;
			boolean result = false;

			synchronized (mServices) {
				service = (Service) mServices.get(intent.getComponent());
			}
			if (service != null) {
				service.onDestroy();
				Context context = service.getBaseContext();
				if (context instanceof ContextImpl) {
					((ContextImpl) context).cleanup();
				}
				synchronized (mServices) {
					mServices.remove(intent.getComponent());
					mServices.notifyAll();
				}
				result = true;
			}

			if (callback != null) {
				Bundle data = new Bundle();
				data.putBoolean("result", result);
				callback.sendResult(data);
			}
		}

		public void bindService(Intent intent, ServiceConnection conn, int flags, IRemoteCallback callback) throws RemoteException {
			Service service;
			Bundle result = new Bundle();

			synchronized (mServices) {
				service = (Service) mServices.get(intent.getComponent());
			}
			if (service != null) {
				IBinder binder = service.onBind(intent);
				result.putBoolean("result", true);
				result.putBinder("binder", binder);
			} else {
				result.putBoolean("result", false);
			}

			callback.sendResult(result);
		}

		public void unbindService(Intent intent) throws RemoteException {
			unbindService(intent, null);
		}

		public void unbindService(Intent intent, IRemoteCallback callback) throws RemoteException {
			Service service;
			boolean result = false;

			synchronized (mServices) {
				service = (Service) mServices.get(intent.getComponent());
			}
			if (service != null) {
				service.onUnbind(intent);
				result = true;
			}

			if (callback != null) {
				Bundle data = new Bundle();
				data.putBoolean("result", result);
				callback.sendResult(data);
			}
		}
	}
}
