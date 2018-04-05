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
 *  Eliza key.
 *  A key has the key itself, a rank, and a list of decompositon rules.
 */
public class Key {
    /** The key */
    String key;
    /** The rank */
    int rank;
    /** The list of decompositions */
    DecompositionList decompositions;

    Key(String key, int rank, DecompositionList decompositions) {
        this.key = key;
        this.rank = rank;
        this.decompositions = decompositions;
    }

    Key() {
        key = null;
        rank = 0;
        decompositions = null;
    }

    public void copy(Key k) {
        key = k.key();
        rank = k.rank();
        decompositions = k.decompositions();
    }

    public void print(int indent) {
        for (int i = 0; i < indent; i++) {
            System.out.print(" ");
        }
        System.out.println("Key: " + key + " " + rank);
        decompositions.print(indent + 2);
    }

    public void printKey(int indent) {
        for (int i = 0; i < indent; i++) {
            System.out.print(" ");
        }
        System.out.println("Key: " + key + " " + rank);
    }

    public String key() {
        return key;
    }

    public int rank() {
        return rank;
    }

    public DecompositionList decompositions() {
        return decompositions;
    }
}

