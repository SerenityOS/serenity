/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

// A test class to be launched in AppCDS mode, has basic+
// coverage of string operations

import sun.hotspot.WhiteBox;

public class HelloStringPlus {
    public static void main(String args[]) {
        // Let's reference the string that is in archive
        String testString1 = "shared_test_string_unique_14325";
        System.out.println("Hello String: " + testString1);

        WhiteBox wb = WhiteBox.getWhiteBox();
        if (!wb.isShared(testString1) && !wb.areSharedStringsIgnored()) {
            throw new RuntimeException("testString1 is not shared");
        }

        // Check other basic string operations
        // Interning and equality
        String[] testArray = new String[] {"shared_", "test_", "string_", "intern_", "12345"};
        String toBeInterned = "";

        StringBuilder sb = new StringBuilder();
        for (String s : testArray) {
            sb.append(s);
        }
        toBeInterned = sb.toString();

        System.out.println("About to intern a string: " + toBeInterned);
        toBeInterned.intern();

        // check equality
        if (testString1.equals(toBeInterned))
            throw new RuntimeException("Equality test 1 failed");

        if (!testString1.equals("shared_test_string" + '_' + "unique_14325"))
            throw new RuntimeException("Equality test 2 failed");

        // Chech the hash code functionality; no special assertions, just make sure
        // no crashe or exception occurs
        System.out.println("testString1.hashCode() = " + testString1.hashCode());

        // Check intern() method for "" string
        String empty = "";
        String empty_interned = empty.intern();
        if (!wb.isShared(empty)) {
           throw new RuntimeException("Empty string should be shared");
        }
        if (empty_interned != empty) {
            throw new RuntimeException("Different string is returned from intern() for empty string");
        }
    }
}
