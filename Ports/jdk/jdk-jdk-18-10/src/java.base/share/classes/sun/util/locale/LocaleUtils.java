/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package sun.util.locale;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collection of static utility methods for Locale support. The
 * methods which manipulate characters or strings support ASCII only.
 */
public final class LocaleUtils {

    private LocaleUtils() {
    }

    /**
     * Compares two ASCII Strings s1 and s2, ignoring case.
     */
    public static boolean caseIgnoreMatch(String s1, String s2) {
        if (s1 == s2) {
            return true;
        }

        int len = s1.length();
        if (len != s2.length()) {
            return false;
        }

        for (int i = 0; i < len; i++) {
            char c1 = s1.charAt(i);
            char c2 = s2.charAt(i);
            if (c1 != c2 && toLower(c1) != toLower(c2)) {
                return false;
            }
        }
        return true;
    }

    static int caseIgnoreCompare(String s1, String s2) {
        if (s1 == s2) {
            return 0;
        }
        return toLowerString(s1).compareTo(toLowerString(s2));
    }

    static char toUpper(char c) {
        return isLower(c) ? (char)(c - 0x20) : c;
    }

    static char toLower(char c) {
        return isUpper(c) ? (char)(c + 0x20) : c;
    }

    /**
     * Converts the given ASCII String to lower-case.
     */
    public static String toLowerString(String s) {
        int len = s.length();
        int idx = 0;
        for (; idx < len; idx++) {
            if (isUpper(s.charAt(idx))) {
                break;
            }
        }
        if (idx == len) {
            return s;
        }

        char[] buf = new char[len];
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            buf[i] = (i < idx) ? c : toLower(c);
        }
        return new String(buf);
    }

    static String toUpperString(String s) {
        int len = s.length();
        int idx = 0;
        for (; idx < len; idx++) {
            if (isLower(s.charAt(idx))) {
                break;
            }
        }
        if (idx == len) {
            return s;
        }

        char[] buf = new char[len];
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            buf[i] = (i < idx) ? c : toUpper(c);
        }
        return new String(buf);
    }

    static String toTitleString(String s) {
        int len;
        if ((len = s.length()) == 0) {
            return s;
        }
        int idx = 0;
        if (!isLower(s.charAt(idx))) {
            for (idx = 1; idx < len; idx++) {
                if (isUpper(s.charAt(idx))) {
                    break;
                }
            }
        }
        if (idx == len) {
            return s;
        }

        char[] buf = new char[len];
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (i == 0 && idx == 0) {
                buf[i] = toUpper(c);
            } else if (i < idx) {
                buf[i] = c;
            } else {
                buf[i] = toLower(c);
            }
        }
        return new String(buf);
    }

    private static boolean isUpper(char c) {
        return c >= 'A' && c <= 'Z';
    }

    private static boolean isLower(char c) {
        return c >= 'a' && c <= 'z';
    }

    static boolean isAlpha(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    static boolean isAlphaString(String s) {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            if (!isAlpha(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    static boolean isNumeric(char c) {
        return (c >= '0' && c <= '9');
    }

    static boolean isNumericString(String s) {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            if (!isNumeric(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    static boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isNumeric(c);
    }

    public static boolean isAlphaNumericString(String s) {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            if (!isAlphaNumeric(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isEmpty(Set<?> set) {
        return set == null || set.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }
}
