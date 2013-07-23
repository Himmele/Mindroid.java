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

package mindroid.app;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Hashtable;
import mindroid.content.ComponentName;
import mindroid.content.Context;
import mindroid.content.Intent;
import mindroid.content.ServiceConnection;
import mindroid.content.pm.ServiceInfo;
import mindroid.os.Bundle;
import mindroid.os.Debug;
import mindroid.os.Handler;
import mindroid.os.HandlerThread;
import mindroid.os.IBinder;
import mindroid.os.Looper;
import mindroid.os.Message;
import mindroid.app.ContextImpl;
import mindroid.util.Log;

public class Process {
	private static final String LOG_TAG = "Process";
	private final String mName;
	private final Hashtable mServices;
	private final HandlerThread mMainThread;
	private ThreadGroup mThreadGroup;
	private MainHandler mMainHandler;
	private Debug mDebug;
	private UncaughtExceptionHandler mUncaughtExceptionHandler = null;
	boolean mUncaughtException = false;
	
	public static interface UncaughtExceptionHandler {
		void uncaughtException(Process process, Throwable e);
	}
	
	public Process(String name) {
		mName = name;
		mThreadGroup = new ThreadGroup("Process {" + name + "}") {
			public void uncaughtException(Thread thread, Throwable e) {
				Log.e(LOG_TAG, "Uncaught exception: " + e.getMessage(), e);
				synchronized (this) {
					if (mUncaughtExceptionHandler != null) {
						mUncaughtExceptionHandler.uncaughtException(Process.this, e);
					}
				}
				synchronized (mServices) {
					mUncaughtException = true;
					mServices.notify();
				}
				super.uncaughtException(thread, e);
			}
		};
		mMainThread = new HandlerThread(mThreadGroup, "Process {" + name + "}");
		mServices = new Hashtable();
		mDebug = Debug.Creator.createInstance();
	}
	
	public void start() {
		Log.i(LOG_TAG, "Starting process " + mName);
		mMainThread.start();
		mDebug.start(this);
		mMainHandler = new MainHandler(mMainThread.getLooper());
	}
	
	public void stop() {
		Enumeration iterator = mServices.keys();
    	while (iterator.hasMoreElements()) {
    		ComponentName component = (ComponentName) iterator.nextElement();
    		Intent intent = new Intent();
    		intent.setComponent(component);
    		Bundle data = new Bundle();
    		data.putObject("intent", intent);
    		Message msg = mMainHandler.obtainMessage(MainHandler.STOP_SERVICE);
    		msg.setData(data);
    		msg.sendToTarget();
    	}
    	synchronized (mServices) {
	    	while (!mServices.isEmpty() && !mUncaughtException) {
	    		try {
	    			// Do not remove the timeout. Had some problems during testing.
					mServices.wait(1000);
				} catch (InterruptedException e) {
				}
	    	}
    	}
    	
    	mDebug.stop();
    	
		mMainThread.quit();
		try {
			mMainThread.join();
		} catch (InterruptedException e) {			
		}
		
		Log.i(LOG_TAG, "Process " + mName + " has been stopped.");
	}
	
	public void createService(Intent intent, ServiceInfo serviceInfo, Message reply) {
		if (!mMainThread.isAlive()) {
			throw new RuntimeException("Process is not running");
		}
		Bundle data = new Bundle();
		data.putObject("intent", intent);
		data.putObject("serviceInfo", serviceInfo);
		data.putObject("reply", reply);
		Message msg = mMainHandler.obtainMessage(MainHandler.CREATE_SERVICE);
		msg.setData(data);
		msg.sendToTarget();
	}
	
	public void startService(Intent intent, int flags, int startId, Message reply) {
		Bundle data = new Bundle();
		data.putObject("intent", intent);
		data.putInt("flags", flags);
		data.putInt("startId", startId);
		data.putObject("reply", reply);
		Message msg = mMainHandler.obtainMessage(MainHandler.START_SERVICE);
		msg.setData(data);
		msg.sendToTarget();
	}
	
	public void stopService(Intent intent) {
		Bundle data = new Bundle();
		data.putObject("intent", intent);
		Message msg = mMainHandler.obtainMessage(MainHandler.STOP_SERVICE);
		msg.setData(data);
		msg.sendToTarget();
	}
	
	public void stopService(Intent intent, Message reply) {
		Bundle data = new Bundle();
		data.putObject("intent", intent);
		data.putObject("reply", reply);
		Message msg = mMainHandler.obtainMessage(MainHandler.STOP_SERVICE);
		msg.setData(data);
		msg.sendToTarget();
	}
	
	public void bindService(Intent intent, ServiceConnection conn, int flags, Message reply) {
		Bundle data = new Bundle();
		data.putObject("intent", intent);
		data.putObject("conn", conn);
		data.putInt("flags", flags);
		data.putObject("reply", reply);
		Message msg = mMainHandler.obtainMessage(MainHandler.BIND_SERVICE);
		msg.setData(data);
		msg.sendToTarget();
	}

	public void unbindService(Intent intent, Message reply) {
		Bundle data = new Bundle();
		data.putObject("intent", intent);
		data.putObject("reply", reply);
		Message msg = mMainHandler.obtainMessage(MainHandler.UNBIND_SERVICE);
		msg.setData(data);
		msg.sendToTarget();
	}
	
	public String getName() {
		return mName;
	}
	
	public HandlerThread getMainThread() {
		return mMainThread;
	}
	
	public ThreadGroup getThreadGroup() {
		return mThreadGroup;
	}
	
	public final void runOnMainThread(Runnable action) {
		mMainHandler.post(action);
	}
	
	public void setUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
		synchronized (this) {
			mUncaughtExceptionHandler = handler;
		}
	}
	
	private class MainHandler extends Handler {
		public static final int CREATE_SERVICE = 0;
		public static final int START_SERVICE = 1;
		public static final int STOP_SERVICE = 2;
		public static final int BIND_SERVICE = 3;
		public static final int UNBIND_SERVICE = 4;
		
		public MainHandler(Looper looper) {
			super(looper);
		}
		
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case CREATE_SERVICE: {
					Intent intent = (Intent) msg.getData().getObject("intent");
					ServiceInfo serviceInfo = (ServiceInfo) msg.getData().getObject("serviceInfo");
					Message reply = (Message) msg.getData().getObject("reply");
					handleCreateService(intent, serviceInfo, reply);
				} break;
				case START_SERVICE: {
					Intent intent = (Intent) msg.getData().getObject("intent");
					int flags = msg.getData().getInt("flags");
					int startId = msg.getData().getInt("startId");
					Message reply = (Message) msg.getData().getObject("reply");
					handleStartService(intent, flags, startId, reply);
				} break;
				case STOP_SERVICE: {
					Intent intent = (Intent) msg.getData().getObject("intent");
					Message reply = null;
					if (msg.getData().containsKey("reply")) {
						reply = (Message) msg.getData().getObject("reply");
					}
					handleStopService(intent, reply);
				} break;
				case BIND_SERVICE: {
					Intent intent = (Intent) msg.getData().getObject("intent");
					ServiceConnection conn = (ServiceConnection) msg.getData().getObject("conn");					
					int flags = msg.getData().getInt("flags");
					Message reply = (Message) msg.getData().getObject("reply");
					handleBindService(intent, conn, flags, reply);
				} break;
				case UNBIND_SERVICE: {
					Intent intent = (Intent) msg.getData().getObject("intent");
					Message reply = (Message) msg.getData().getObject("reply");
					handleUnbindService(intent, reply);
				} break;
			}
	    }
	}	
	
	private void handleCreateService(Intent intent, ServiceInfo serviceInfo, Message reply) {
        Service service = null;
        String componentName = null;
        try {
        	componentName = intent.getComponent().getPackageName() + "." + intent.getComponent().getClassName();
        	if (serviceInfo.systemService) {
	        	Class clazz = Class.forName(componentName);
				service = (Service) clazz.newInstance();
        	} else {
				URLClassLoader classLoader = new URLClassLoader(new URL[]{ new File(serviceInfo.applicationInfo.fileName).toURL() },
						getClass().getClassLoader());
				Class clazz = Class.forName(componentName, true, classLoader);
				service = (Service) clazz.newInstance();
        	}
        	if (service != null) {
				service.attach(new ContextImpl(mMainThread, intent.getComponent()), this, intent.getComponent());
				mServices.put(intent.getComponent(), service);
				service.onCreate();
				reply.arg1 = 0;
				reply.sendToTarget();
        	} else {
        		reply.arg1 = -1;
        		reply.sendToTarget();
        	}
        } catch (Exception e) {
        	reply.arg1 = -1;
        	reply.sendToTarget();
        	Log.e(LOG_TAG, "Cannot create service " + componentName + ": " + e, e);
        	throw new RuntimeException(e.getMessage(), e.getCause());
        }
    }
	
	private void handleStartService(Intent intent, int flags, int startId, Message reply) {
		Service service = (Service) mServices.get(intent.getComponent());
		if (service != null) {
	        service.onStartCommand(intent, flags, startId);
	        reply.sendToTarget();
		}
    }
	
	private void handleStopService(Intent intent, Message reply) {
		Service service = (Service) mServices.get(intent.getComponent());
		if (service != null) {
			service.onDestroy();
			Context context = service.getBaseContext();
			if (context instanceof ContextImpl) {
				((ContextImpl) context).cleanup();
			}
	        mServices.remove(intent.getComponent());
	        if (reply != null) {
	        	reply.sendToTarget();
	        }
		}
        synchronized (mServices) {
        	mServices.notify();
        }
    }
	
	private void handleBindService(Intent intent, ServiceConnection conn, int flags, Message reply) {
		Service service = (Service) mServices.get(intent.getComponent());
		if (service != null) {
	        IBinder binder = service.onBind(intent);
	        reply.getData().putObject("binder", binder);
	        reply.sendToTarget();
		}
    }
	
	private void handleUnbindService(Intent intent, Message reply) {
		Service service = (Service) mServices.get(intent.getComponent());
		if (service != null) {
	        service.onUnbind(intent);
	        reply.sendToTarget();
		}
    }
}
