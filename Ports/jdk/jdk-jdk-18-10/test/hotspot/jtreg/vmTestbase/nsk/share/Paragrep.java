/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package nsk.share;

import java.util.*;
import java.util.regex.*;

/**
 * Weak emulator of perl's grep function with very small functionality.
 * This class does not use java.util.regexp classes which appear in
 * JDK1.4 API.
 *
 * @see Grep
 */

public class Paragrep {

    String[] stringArray;
    /**
     * Takes String array as character sequence for matching the pattern.
     */
    public Paragrep (String[] stringArray) {
        this.stringArray = stringArray;
    }

    /**
     * Returns number of non-interleaved occurences of the pattern string.
     */
    public int find (String pattern) {
        if (pattern.length() == 0) {
            throw new Failure("Empty string as input parameter for Grep.find(pattern) method");
        }
        int counter = 0;
        for (int i = 0; i < stringArray.length; i++) {

            String string = stringArray[i];
            if (string != null) {
                // Find all non-interleaved occurences of pattern in this string
                for (int ind = 0; ind < string.length(); ) {
                    int k = 0;
                    if ((k = string.indexOf(pattern, ind)) >= 0) {
                       counter++;
                       ind = k + pattern.length();
                    } else {
                       break;
                    }
                }
            }
        }
        return counter;
    }

    /**
     * Returns all string in <code>stringArray</code> which have
     * occurences of the pattern string.
     */
    public String[] findStrings (String pattern) {
        if (pattern.length() == 0) {
            throw new Failure("Empty string as input parameter for Grep.find(pattern) method");
        }
        Vector<String> v = new Vector<String>();
        for (int i = 0; i < stringArray.length; i++) {
            String string = stringArray[i];
            if (string != null && string.indexOf(pattern) >= 0) {
               v.add(string);
            }
        }
        String[] result = new String[v.size()];
        v.toArray(result);
        return result;
    }

    /**
     * Returns first string of stringArray which contains
     * the pattern string or empty string othrewise.
     */
    public String findFirst (String pattern) {
        if (pattern.length() == 0) {
            throw new Failure("Empty string as input parameter for Paragrep.findFirst(pattern) method");
        }
        String result = "";
        for (int i = 0; i < stringArray.length; i++) {
            String string = stringArray[i];
            if (string != null) {
                if (string.indexOf(pattern) >= 0) {
                    result = string;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Returns first string of stringArray which contains
     * all of the pattern strings or empty string otherwise.
     */
    public String findFirst (Vector<String> patternVector) {
        if (patternVector.isEmpty()) {
            throw new Failure("Empty vector as input parameter for Paragrep.findFirst(patternVector) method");
        }
        String[] patterns = new String[patternVector.size()];
        patternVector.toArray(patterns);
        String result = "";
        for (int i = 0; i < stringArray.length; i++) {
            String string = stringArray[i];
            if (string != null && string.length() > 0) {
                for (int j = 0; j < patterns.length; j++) {
                    String pattern = patterns[j];
                    if (string.indexOf(pattern) >= 0) {
                        if (j + 1 == patterns.length) {
                            // found all patterns in the current string
                            result = string;
                            i = stringArray.length;
                        }
                    } else {
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns count of strings in stringArray which contain
     * all of the pattern strings.
     */
    public int find (Vector<String> patternVector) {
        if (patternVector.isEmpty()) {
            throw new Failure("Empty vector as input parameter for Paragrep.find(patternVector) method");
        }
        String[] patterns = new String[patternVector.size()];
        patternVector.toArray(patterns);
        int counter = 0;

        for (int i = 0; i < stringArray.length; i++) {
            String string = stringArray[i];
            if (string != null && string.length() > 0) {
                for (int j = 0; j < patterns.length; j++) {
                    String pattern = patterns[j];
                    if (string.indexOf(pattern) >= 0) {
                        if (j + 1 == patterns.length) {
                            // found all patterns in the current string
                            counter++;
                        }
                    } else {
                        break;
                    }
                }
            }
        }
        return counter;
    }
}
