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
 * Emulator of perl's grep function.
 * This class uses java.util.regexp classes which appear in
 * JDK1.4 API. This implies the restriction for this class
 * to not be used with the tests against JDKs prior to 1.4.
 *
 * @see java.util.regex.Pattern
 * @see java.util.regex.Matcher
 */

public class Grep {

    String[] stringArray;
    /**
     * Takes String array as character sequence for matching the pattern.
     */
    public Grep (String[] stringArray) {
        this.stringArray = stringArray;
    }

    /**
     * Returns number of non-interleaved occurences of groups which match the pattern.
     */
    public int find (String regExpPattern) {
        if (regExpPattern.length() == 0) {
            throw new Failure("Empty string as input parameter for Grep.find(regExpPattern) method");
        }
        Pattern pattern = Pattern.compile(regExpPattern);
        int counter = 0;
        for (int i = 0; i < stringArray.length; i++) {

            String string = stringArray[i];
            if (string != null) {
                // Create matcher for this string
                Matcher matcher = pattern.matcher(string);

                // Find all non-interleaved occurences of pattern in this string
                for (int ind = 0; ind < string.length(); ) {
                    if (matcher.find(ind)) {
                       counter++;
                       ind = matcher.end();
                    } else {
                       break;
                    }
                }
            }
        }
        return counter;
    }

    /**
     * Returns first string of stringArray with group which matches
     * the pattern or empty string othrewise.
     */
    public String findFirst (String regExpPattern) {
        if (regExpPattern.length() == 0) {
            throw new Failure("Empty string as input parameter for Grep.findFirst(regExpPattern) method");
        }
        Pattern pattern = Pattern.compile(regExpPattern);
        String result = "";
        for (int i = 0; i < stringArray.length; i++) {

            String string = stringArray[i];
            if (string != null) {
                // Create matcher for this string
                Matcher matcher = pattern.matcher(string);
                if (matcher.find()) {
                    result = string;
                    break;
                }
            }
        }
        return result;
    }
}
