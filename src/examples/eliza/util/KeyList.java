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

import java.util.ArrayList;

/**
 *  Eliza key list.
 */
public class KeyList extends ArrayList<Key> {
    public boolean add(String key, int rank, DecompositionList decompositions) {
        return super.add(new Key(key, rank, decompositions));
    }

    public void print(int indent) {
        for (int i = 0; i < size(); i++) {
            Key k = get(i);
            k.print(indent);
        }
    }

    Key getKey(String s) {
        for (int i = 0; i < size(); i++) {
            Key key = get(i);
            if (s.equals(key.key())) {
                return key;
            }
        }
        return null;
    }

    /**
     *  Break the string s into words.
     *  For each word, if isKey is true, push the key
     *  into the stack.
     */
    public void build(KeyStack stack, String s) {
        stack.reset();
        s = Strings.trim(s);
        String lines[] = new String[2];
        Key k;
        while (Strings.match(s, "* *", lines)) {
            k = getKey(lines[0]);
            if (k != null) {
                stack.pushKey(k);
            }
            s = lines[1];
        }
        k = getKey(s);
        if (k != null) {
            stack.pushKey(k);
        }
    }
}
