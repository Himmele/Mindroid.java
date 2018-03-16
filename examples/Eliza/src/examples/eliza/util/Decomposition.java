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

import java.lang.Math;

/**
 *  Eliza decomposition rule
 */
public class Decomposition {
    /** The decomp pattern */
    String pattern;
    /** The memory flag */
    boolean memory;
    /** The reassembly list */
    ReassemblyList reassemblyList;
    /** The reassembly index */
    int reassemblyIndex;

    Decomposition(String pattern, boolean memory, ReassemblyList reassemblyList) {
        this.pattern = pattern;
        this.memory = memory;
        this.reassemblyList = reassemblyList;
        this.reassemblyIndex = 100;
    }

    public void print(int indent) {
        String m = memory ? "true" : "false";
        for (int i = 0; i < indent; i++) {
            System.out.print(" ");
        }
        System.out.println("Decomposition: " + pattern + " " + m);
        reassemblyList.print(indent + 2);
    }

    public String pattern() {
        return pattern;
    }

    public boolean memory() {
        return memory;
    }

    public String nextReassemblyRule() {
        if (reassemblyList.size() == 0) {
            return null;
        }
        return reassemblyList.get(reassemblyIndex);
    }

    /**
     *  Step to the next reassembly rule.
     *  If memory is true, pick a random rule.
     */
    public void stepReassemblyRule() {
        int size = reassemblyList.size();
        if (memory) {
            reassemblyIndex = (int)(Math.random() * size);
        }
        reassemblyIndex++;
        if (reassemblyIndex >= size) {
            reassemblyIndex = 0;
        }
    }
}
