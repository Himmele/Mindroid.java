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
 *  A stack of keys in rank order.
 */
public class KeyStack {
    /** The stack size */
    static final int SIZE = 20;
    /** The key stack */
    Key stack[] = new Key[SIZE];
    /** The key stack top */
    int top = 0;

    public void print() {
        System.out.println("Stack: " + top);
        for (int i = 0; i < top; i++) {
            stack[i].printKey(0);
        }
    }

    public int top() {
        return top;
    }

    public void reset() {
        top = 0;
    }

    public Key key(int n) {
        if (n < 0 || n >= top) {
            return null;
        }
        return stack[n];
    }

    public void pushKey(Key key) {
        if (key == null) {
            return;
        }
        int i;
        for (i = top; i > 0; i--) {
            if (key.rank() > stack[i - 1].rank()) {
                stack[i] = stack[i - 1];
            } else {
                break;
            }
        }
        stack[i] = key;
        top++;
    }
}
