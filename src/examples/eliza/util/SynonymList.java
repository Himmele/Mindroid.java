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
 *  Eliza synonym list.
 */
public class SynonymList extends ArrayList<WordList> {
    public boolean add(WordList words) {
        return super.add(words);
    }

    public void print(int indent) {
        for (int i = 0; i < size(); i++) {
            for (int j = 0; j < indent; j++) {
                System.out.print(" ");
            }
            System.out.print("Synonyms: ");
            WordList wl = get(i);
            wl.print(indent);
        }
    }

    /**
     *  Find a synonym word list.
     */
    public WordList find(String s) {
        for (int i = 0; i < size(); i++) {
            WordList wl = get(i);
            if (wl.contains(s)) {
                return wl;
            }
        }
        return null;
    }
    
    /**
     *  Decomposition match, if decomposition has no synonyms,
     *  do a regular match.
     *  Otherwise, try all synonyms.
     */
    boolean matchDecomposition(String string, String pattern, String lines[]) {
        if (!Strings.match(pattern, "*@* *", lines)) {
            //  No synonyms in decomposition pattern.
            return Strings.match(string, pattern, lines);
        }
        //  Decomposition pattern has synonym.
        String first = lines[0];
        String synonym = lines[1];
        String rest = " " + lines[2];
        //  Look up the synonym.
        WordList wl = find(synonym);
        if (wl == null) {
            System.out.println("Cannot find synonym list for " + synonym);
            return false;
        }
        //  Try each synonym individually.
        for (int i = 0; i < wl.size(); i++) {
            //  Make a modified pattern
            pattern = first + wl.get(i) + rest;
            if (Strings.match(string, pattern, lines)) {
                int n = Strings.count(first, '*');
                //  Make room for the synonym in the match list.
                for (int j = lines.length - 2; j >= n; j--) {
                    lines[j + 1] = lines[j];
                }
                //  Put the synonym in the match list.
                lines[n] = wl.get(i);
                return true;
            }
        }
        return false;
    }
}
