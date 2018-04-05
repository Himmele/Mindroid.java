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
 *  Eliza string functions.
 */
public class Strings {
    /** The digits. */
    static final String NUMBERS = "0123456789";

    /**
     *  An approximate string matching method that looks
     *  for a match between the string and the pattern.
     *  Return count of matching characters before * or #.
     *  Return -1 if strings do not match.
     */
    public static int amatch(String s, String pattern) {
        int count = 0;
        int i = 0;  // s index.
        int j = 0;  // pattern index.
        while (i < s.length() && j < pattern.length()) {
            char p = pattern.charAt(j);
            // Stop if pattern is * or #.
            if (p == '*' || p == '#') {
                return count;
            }
            if (s.charAt(i) != p) {
                return -1;
            }
            i++; j++; count++;
        }
        return count;
    }

    /**
     *  Search in successive positions of the string,
     *  looking for a match to the pattern.
     *  Return the string position in s in case of a match,
     *  or -1 for no match.
     */
    public static int findPattern(String s, String pattern) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (amatch(s.substring(i), pattern) >= 0) {
                return count;
            }
            count++;
        }
        return -1;
    }

    /**
     *  Returns the number of digits at the beginning of s.
     */
    public static int findNumbers(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (NUMBERS.indexOf(s.charAt(i)) == -1) {
                return count;
            }
            count++;
        }
        return count;
    }

    /**
     *  Match the string against a pattern and fills in
     *  matches array with the pieces that matched * and #
     */
    private static boolean matchA(String s, String pattern, String matches[]) {
        int i = 0;   // s index.
        int j = 0;   // matches index.
        int pos = 0; // pattern index.
        while (pos < pattern.length() && j < matches.length) {
            char p = pattern.charAt(pos);
            if (p == '*') {
                int n;
                if (pos + 1 == pattern.length()) {
                    // * is the last char in pattern.
                    // n is remaining string length.
                    n = s.length() - i;
                } else {
                    // * is not last char in pattern.
                    // find using remaining pattern.
                    n = findPattern(s.substring(i), pattern.substring(pos + 1));
                }
                if (n < 0) {
                    return false;
                }
                matches[j++] = s.substring(i, i + n);
                i += n;
                pos++;
            } else if (p == '#') {
                int n = findNumbers(s.substring(i));
                matches[j++] = s.substring(i, i + n);
                i += n;
                pos++;
            } else {
                int n = amatch(s.substring(i), pattern.substring(pos));
                if (n <= 0) {
                    return false;
                }
                i += n;
                pos += n;
            }
        }
        if (i >= s.length() && pos >= pattern.length()) return true;
        return false;
    }

    /*
     *  This version is clearer, but hopelessly slow
     */
    private static boolean matchB(String s, String pattern, String matches[]) {
        int j = 0; // matches index.
        while (pattern.length() > 0 && s.length() >= 0 && j < matches.length) {
            char p = pattern.charAt(0);
            if (p == '*') {
                int n;
                if (pattern.length() == 1) {
                    // * is the last char in pattern.
                    // n is remaining string length.
                    n = s.length();
                } else {
                    // * is not last char in pattern.
                    // find using remaining pattern.
                    n = findPattern(s, pattern.substring(1));
                }
                if (n < 0) {
                    return false;
                }
                matches[j++] = s.substring(0, n);
                s = s.substring(n);
                pattern = pattern.substring(1);
            } else if (p == '#') {
                int n = findNumbers(s);
                matches[j++] = s.substring(0, n);
                s = s.substring(n);
                pattern = pattern.substring(1);
            } else {
                int n = amatch(s, pattern);
                if (n <= 0) {
                    return false;
                }
                s = s.substring(n);
                pattern = pattern.substring(n);
            }
        }
        if (s.length() == 0 && pattern.length() == 0) {
            return true;
        }
        return false;
    }

    public static boolean match(String s, String pattern, String matches[]) {
        return matchA(s, pattern, matches);
    }

    /**
     *  Translates corresponding characters in source to destination.
     */
    public static String translate(String string, String source, String destination) {
        if (source.length() != destination.length()) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < source.length(); i++) {
            string = string.replace(source.charAt(i), destination.charAt(i));
        }
        return string;
    }

    /**
     *  Compresses its input by:
     *  (1) Dropping space before space, comma, and period;
     *  (2) Adding space before question, if char before is not a space; and
     *  (3) copying all others
     */
    public static String compress(String s) {
        String destination = "";
        if (s.length() == 0) {
            return s;
        }
        char c = s.charAt(0);
        for (int i = 1; i < s.length(); i++) {
            if (c == ' ' &&
                 ((s.charAt(i) == ' ') ||
                 (s.charAt(i) == ',') ||
                 (s.charAt(i) == '.'))) {
                // Do nothing.
            } else if (c != ' ' && s.charAt(i) == '?') {
                destination += c + " ";
            } else {
                destination += c;
            }
            c = s.charAt(i);
        }
        destination += c;
        return destination;
    }

    /**
     *  Trim off leading spaces.
     */
    public static String trim(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != ' ') {
                return s.substring(i);
            }
        }
        return "";
    }

    /**
     *  Pad by ensuring there are spaces before and after the sentence.
     */
    public static String pad(String s) {
        if (s.length() == 0) {
            return " ";
        }
        char first = s.charAt(0);
        char last = s.charAt(s.length() - 1);
        if (first == ' ' && last == ' ') return s;
        if (first == ' ' && last != ' ') return s + " ";
        if (first != ' ' && last == ' ') return " " + s;
        if (first != ' ' && last != ' ') return " " + s + " ";
        return s;
    }

    /**
     *  Count number of occurrences of c in s.
     */
    public static int count(String s, char c) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) count++;
        }
        return count;
    }
}
