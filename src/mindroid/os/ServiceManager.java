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

import java.util.HashMap;
import mindroid.app.Service;

public final class ServiceManager {
	private static HashMap<String, Service> sServices = new HashMap<String, Service>();
	
	public static Service getService(String name) {
		synchronized (sServices) {
			Service service = sServices.get(name);
			return service;
		}
	}
	
	public static boolean addService(String name, Service service) {
		synchronized (sServices) {
			if (!sServices.containsKey(name)) {
				sServices.put(name, service);
				return true;
			}
			return false;
		}
	}
	
	public static boolean removeService(String name) {
		synchronized (sServices) {
			if (sServices.containsKey(name)) {
				sServices.remove(name);
				return true;
			}
			return false;
		}
	}
}
