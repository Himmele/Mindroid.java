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

import java.io.*;

import mindroid.util.Log;

/**
 *  Eliza main class.
 *  Performs the input transformations.
 */
 public class Eliza {
    private static final String LOG_TAG = "Eliza";

    /** The key list */
    private KeyList keys = new KeyList();
    /** The synonym list */
    private SynonymList synonyms = new SynonymList();
    /** The pre list */
    private PrePostList preList = new PrePostList();
    /** The post list */
    private PrePostList postList = new PrePostList();
    /** Welcome string */
    private String welcome = "Hello.";
    /** Goodbye string */
    private String goodbye = "Goodbye.";
    /** Quit list */
    private WordList quit = new WordList();
    /** Key stack */
    private KeyStack keyStack = new KeyStack();
    /** Memory */
    private Memory memory = new Memory();

    private DecompositionList decompositionList;
    private ReassemblyList reassemblyList;

    public Eliza() throws IOException {
        BufferedReader reader = null;
        try {
            InputStream inputStream = null;
            try {
                inputStream = this.getClass().getResourceAsStream("/examples/eliza/Eliza.cfg");
            } catch (Exception e) {
            }
            if (inputStream == null) {
                inputStream = new FileInputStream("examples/Eliza/res/examples/eliza/Eliza.cfg");
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null)   {
                processConfigurationRule(line);
            }
        } catch (IOException e) {
            Log.println('E', LOG_TAG, "Cannot load Eliza configuration", e);
            throw e;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private void processConfigurationRule(String s) {
        String lines[] = new String[4];

        if (Strings.match(s, "*reasmb: *", lines)) {
            if (reassemblyList == null) {
                System.out.println("Error: no reassembly list");
                return;
            }
            reassemblyList.add(lines[1]);
        }
        else if (Strings.match(s, "*decomp: *", lines)) {
            if (decompositionList == null) {
                System.out.println("Error: no decomposition list");
                return;
            }
            reassemblyList = new ReassemblyList();
            String tmp = new String(lines[1]);
            if (Strings.match(tmp, "$ *", lines)) {
                decompositionList.add(lines[0], true, reassemblyList);
            } else {
                decompositionList.add(tmp, false, reassemblyList);
            }
        }
        else if (Strings.match(s, "*key: * #*", lines)) {
            decompositionList = new DecompositionList();
            reassemblyList = null;
            int n = 0;
            if (lines[2].length() != 0) {
                try {
                    n = Integer.parseInt(lines[2]);
                } catch (NumberFormatException e) {
                    System.out.println("Number is wrong in key: " + lines[2]);
                }
            }
            keys.add(lines[1], n, decompositionList);
        }
        else if (Strings.match(s, "*key: *", lines)) {
            decompositionList = new DecompositionList();
            reassemblyList = null;
            keys.add(lines[1], 0, decompositionList);
        }
        else if (Strings.match(s, "*synon: * *", lines)) {
            WordList words = new WordList();
            words.add(lines[1]);
            s = lines[2];
            while (Strings.match(s, "* *", lines)) {
                words.add(lines[0]);
                s = lines[1];
            }
            words.add(s);
            synonyms.add(words);
        }
        else if (Strings.match(s, "*pre: * *", lines)) {
            preList.add(lines[1], lines[2]);
        }
        else if (Strings.match(s, "*post: * *", lines)) {
            postList.add(lines[1], lines[2]);
        }
        else if (Strings.match(s, "*initial: *", lines)) {
            welcome = lines[1];
        }
        else if (Strings.match(s, "*final: *", lines)) {
            goodbye = lines[1];
        }
        else if (Strings.match(s, "*quit: *", lines)) {
            quit.add(" " + lines[1] + " ");
        }
        else {
            System.out.println("Unrecognized input: " + s);
        }
    }

    public String talk(String s) {
        String reply;
        //  Do some input transformations.
        s = Strings.translate(s, "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                                 "abcdefghijklmnopqrstuvwxyz");
        s = Strings.translate(s, "@#$%^&*()_-+=~`{[}]|:;<>\\\"",
                                 "                          "  );
        s = Strings.translate(s, ",?!", "...");
        //  Remove multiple spaces.
        s = Strings.compress(s);
        String lines[] = new String[2];
        //  Break apart sentences, and do each separately.
        while (Strings.match(s, "*.*", lines)) {
            reply = sentence(lines[0]);
            if (reply != null) {
                return reply;
            }
            s = Strings.trim(lines[1]);
        }
        if (s.length() != 0) {
            reply = sentence(s);
            if (reply != null) {
                return reply;
            }
        }
        //  Nothing matched, so try memory.
        String m = memory.get();
        if (m != null) {
            return m;
        }

        //  No memory, reply with xnone.
        Key key = keys.getKey("xnone");
        if (key != null) {
            Key dummy = null;
            reply = decompose(key, s, dummy);
            if (reply != null) {
                return reply;
            }
        }
        //  No xnone, just say anything.
        return "I am at a loss for words.";
    }

    /**
     *  Process a sentence.
     *  (1) Make pre transformations.
     *  (2) Check for quit word.
     *  (3) Scan sentence for keys, build key stack.
     *  (4) Try decompositions for each key.
     */
    private String sentence(String s) {
        s = preList.translate(s);
        s = Strings.pad(s);
        if (quit.contains(s)) {
            return goodbye;
        }
        keys.build(keyStack, s);
        for (int i = 0; i < keyStack.top(); i++) {
            Key gotoKey = new Key();
            String reply = decompose(keyStack.key(i), s, gotoKey);
            if (reply != null) {
                return reply;
            }
            //  If decomposition returned gotoKey, try it.
            while (gotoKey.key() != null) {
                reply = decompose(gotoKey, s, gotoKey);
                if (reply != null) {
                    return reply;
                }
            }
        }
        return null;
    }

    /**
     *  Decompose a string according to the given key.
     *  Try each decomposition rule in order.
     *  If it matches, assemble a reply and return it.
     *  If assembly fails, try another decomposition rule.
     *  If assembly is a goto rule, return null and give the key.
     *  If assembly succeeds, return the reply;
     */
    private String decompose(Key key, String s, Key gotoKey) {
        String reply[] = new String[10];
        for (int i = 0; i < key.decompositions().size(); i++) {
            Decomposition d = (Decomposition)key.decompositions().get(i);
            String pattern = d.pattern();
            if (synonyms.matchDecomposition(s, pattern, reply)) {
                String rep = assemble(d, reply, gotoKey);
                if (rep != null) {
                    return rep;
                }
                if (gotoKey.key() != null) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     *  Assembly a reply from a decomposition rule and the input.
     *  If the reassembly rule is a goto rule, return null and give the gotoKey to use.
     *  Otherwise return the response.
     */
    private String assemble(Decomposition d, String reply[], Key gotoKey) {
        String lines[] = new String[3];
        d.stepReassemblyRule();
        String rule = d.nextReassemblyRule();
        if (Strings.match(rule, "goto *", lines)) {
            // Goto rule, set gotoKey and return false.
            gotoKey.copy(keys.getKey(lines[0]));
            if (gotoKey.key() != null) {
                return null;
            }
            System.out.println("Goto rule did not match key: " + lines[0]);
            return null;
        }
        String tmp = "";
        while (Strings.match(rule, "* (#)*", lines)) {
            // Reassembly rule with number substitution.
            rule = lines[2];
            int n = 0;
            try {
                n = Integer.parseInt(lines[1]) - 1;
            } catch (NumberFormatException e) {
                System.out.println("Number is wrong in reassembly rule " + lines[1]);
            }
            if (n < 0 || n >= reply.length) {
                System.out.println("Substitution number is bad " + lines[1]);
                return null;
            }
            reply[n] = postList.translate(reply[n]);
            tmp += lines[0] + " " + reply[n];
        }
        tmp += rule;
        if (d.memory()) {
            memory.save(tmp);
            return null;
        }
        return tmp;
    }
    
    public void print() {
        keys.print(0);
        synonyms.print(0);
        preList.print(0);
        postList.print(0);
        quit.print(0);
    }
}
