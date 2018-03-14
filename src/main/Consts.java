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

import mindroid.content.ComponentName;

public class Consts {
    public static final ComponentName SERVICE_MANAGER = new ComponentName("mindroid.os", "ServiceManager");
    public static final ComponentName PACKAGE_MANAGER = new ComponentName("mindroid.content.pm", "PackageManagerService");
    public static final ComponentName LOGGER_SERVICE = new ComponentName("mindroid.util.logging", "Logger");
    public static final ComponentName LOCATION_MANAGER_SERVICE = new ComponentName("mindroid.location", "LocationManagerService");
    public static final ComponentName TELEPHONY_SERVICE = new ComponentName("mindroid.telephony", "TelephonyManagerService");
}
