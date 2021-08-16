/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package utils;

public class Utils {

    public static void log(String field, String val1, String val2) {
        System.out.println(field + " mismatch. " + val1 + " vs " + val2);
    }

    public static boolean compareStrings(String s1, String s2) {

        if (s1 != null && s1.equals(Consts.UNKNOWN)
                || s2 != null && s2.equals(Consts.UNKNOWN)) {
            return true;
        }

        if (s1 == null && s2 != null || s1 != null && s2 == null) {
            return false;
        }

        if (s1 == null || s2 == null) {
            return true;
        }
        return s1.equals(s2);
    }

    public static void sleep() {
        try {
            while (true) {
                Thread.sleep(Long.MAX_VALUE);
            }
        } catch (InterruptedException e) {
        }
    }
}
