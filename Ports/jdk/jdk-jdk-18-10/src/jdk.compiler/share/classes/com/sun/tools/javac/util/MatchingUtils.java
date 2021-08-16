/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.tools.javac.util;

import java.util.regex.Pattern;
import javax.lang.model.SourceVersion;

/**Utilities to convert an import-like string to a regexp.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class MatchingUtils {

    private static final String allMatchesString = ".*";
    private static final Pattern allMatches = Pattern.compile(allMatchesString);

    /**
     * Return true if the argument string is a valid import-style
     * string specifying claimed annotations; return false otherwise.
     */
    public static boolean isValidImportString(String s) {
        if (s.equals("*"))
            return true;

        boolean valid = true;
        String t = s;
        int index = t.indexOf('*');

        if (index != -1) {
            // '*' must be last character...
            if (index == t.length() -1) {
                // ... any and preceding character must be '.'
                if ( index-1 >= 0 ) {
                    valid = t.charAt(index-1) == '.';
                    // Strip off ".*$" for identifier checks
                    t = t.substring(0, t.length()-2);
                }
            } else
                return false;
        }

        // Verify string is off the form (javaId \.)+ or javaId
        if (valid) {
            String[] javaIds = t.split("\\.", t.length()+2);
            for(String javaId: javaIds)
                valid &= SourceVersion.isIdentifier(javaId);
        }
        return valid;
    }

    public static String validImportStringToPatternString(String s) {
        if (s.equals("*")) {
            return allMatchesString;
        } else {
            String s_prime = s.replace(".", "\\.");

            if (s_prime.endsWith("*")) {
                s_prime =  s_prime.substring(0, s_prime.length() - 1) + ".+";
            }

            return s_prime;
        }
    }

    public static Pattern validImportStringToPattern(String s) {
        String pattern = validImportStringToPatternString(s);

        if (pattern == allMatchesString) {
            return allMatches;
        } else {
            return Pattern.compile(pattern);
        }
    }

}
