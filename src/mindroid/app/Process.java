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

import java.util.Map;
import java.util.HashMap;
import mindroid.os.Handler;
import mindroid.os.HandlerThread;
import mindroid.os.Looper;
import mindroid.os.Message;

public class Process {
	private final Map<String, Service> mServices;
	private final HandlerThread mMainThread;
	private MainHandler mMainHandler;
	private int mStartId;
	
	public Process() {
		mMainThread = new HandlerThread();
		mServices = new HashMap<String, Service>();
		mStartId = 0;
	}
	
	public void start() {
		mMainThread.start();
		mMainHandler = new MainHandler(mMainThread.getLooper());
	}
	
	public void stop() {
		for (Map.Entry<String, Service> entry : mServices.entrySet()) {
			String fqcn = (String) entry.getKey();
    		mMainHandler.obtainMessage(MainHandler.STOP_SERVICE, fqcn).sendToTarget();
		}

	    synchronized (mServices) {
	    	while (!mServices.isEmpty()) {
	    		try {
					mServices.wait();
				} catch (InterruptedException e) {
				}
	    	}
    	}
    	
		mMainThread.quit();
		try {
			mMainThread.join();
		} catch (InterruptedException e) {			
		}
	}
	
	public void deployService(String fullyQualifiedClassName) {
		if (!mMainThread.isAlive()) {
			throw new RuntimeException("Process is not running");
		}
		mMainHandler.obtainMessage(MainHandler.CREATE_SERVICE, fullyQualifiedClassName).sendToTarget();
	}
	
	private class MainHandler extends Handler {
		public static final int CREATE_SERVICE = 0;
		public static final int START_SERVICE = 1;
		public static final int STOP_SERVICE = 2;
		
		public MainHandler(Looper looper) {
			super(looper);
		}
		
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case CREATE_SERVICE: {
					handleCreateService((String) msg.obj);
				} break;
				case START_SERVICE: {
					handleStartService((String) msg.obj, msg.arg1);
				} break;
				case STOP_SERVICE: {
					handleStopService((String) msg.obj);
				} break;
			}
	    }
	}	
	
	private void handleCreateService(String fqcn) {
        Service service = null;
        try {
        	Class clazz = Class.forName(fqcn);
			service = (Service) clazz.newInstance();
			service.attach(this, fqcn);
			mServices.put(fqcn, service);
        } catch (Exception e) {
        	return;
        }
        service.onCreate();
        
        mMainHandler.obtainMessage(MainHandler.START_SERVICE, mStartId++, 0, fqcn).sendToTarget();
    }
	
	private void handleStartService(String fqcn, int startId) {
		Service service = (Service) mServices.get(fqcn);
        service.onStart(startId);
    }
	
	private void handleStopService(String fqcn) {
		Service service = (Service) mServices.get(fqcn);
        service.onDestroy();
        mServices.remove(fqcn);
        synchronized (mServices) {
        	mServices.notify();
        }
    }
	
	void stopService(String fqcn) {
		mMainHandler.obtainMessage(MainHandler.STOP_SERVICE, fqcn).sendToTarget();
	}
}
