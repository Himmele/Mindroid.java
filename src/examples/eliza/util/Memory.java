/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * 
 *  Charles Hayden's implementation of ELIZA (by Joseph Weizenbaum): http://www.chayden.net/eliza/Eliza.html.
 */

package examples.eliza.util;

/**
 *  Eliza memory class
 */
public class Memory {
    /** The memory size */
    private static final int SIZE = 20;
    /** The memory */
    String memory[] = new String[SIZE];
    /** The memory top */
    int top = 0;

    public void save(String str) {
        if (top < SIZE) {
            memory[top++] = new String(str);
        }
    }

    public String get() {
        if (top == 0) return null;
        String s = memory[0];
        for (int i = 0; i < top - 1; i++) {
            memory[i] = memory[i+1];
        }
        top--;
        return s;
    }
}
