/*
 * Copyright (C) 2006 The Android Open Source Project
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

package mindroid.util;

public class Logger {	
	private static final int BUFFER_SIZE_256_KB = 262144;
	private CircularLogBuffer mLogBuffer = new CircularLogBuffer(BUFFER_SIZE_256_KB);
	
	public int println(int bufferId, int priority, String tag, String msg) {		
		mLogBuffer.insertLogMessage(priority, tag, msg);
		return 0;
	}
	
	public CircularLogBuffer getLogBuffer() {
		return mLogBuffer;
	}
}
