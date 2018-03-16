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
 *  Eliza pre-post list.
 *  This list of pre-post entries is used to perform word transformations.
 */
public class PrePostList extends ArrayList<PrePostEntry> {
    public boolean add(String source, String destination) {
        return super.add(new PrePostEntry(source, destination));
    }

    public void print(int indent) {
        for (int i = 0; i < size(); i++) {
            PrePostEntry p = get(i);
            p.print(indent);
        }
    }

    /**
     *  Map a string.
     *  If string matches a source string on the list,
     *  return he corresponding destination.
     *  Otherwise, return the input.
     */
    String map(String string) {
        for (int i = 0; i < size(); i++) {
            PrePostEntry p = get(i);
            if (string.equals(p.source())) {
                return p.destination();
            }
        }
        return string;
    }

    /**
     *  Translate a string s.
     *  (1) Trim spaces off.
     *  (2) Break s into words.
     *  (3) For each word, substitute matching source word with destination.
     */
    public String translate(String s) {
        String lines[] = new String[2];
        String tmp = Strings.trim(s);
        s = "";
        while (Strings.match(tmp, "* *", lines)) {
            s += map(lines[0]) + " ";
            tmp = Strings.trim(lines[1]);
        }
        s += map(tmp);
        return s;
    }
}
