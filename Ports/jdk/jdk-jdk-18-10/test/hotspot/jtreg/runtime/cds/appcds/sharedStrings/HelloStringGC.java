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

import sun.hotspot.WhiteBox;

public class HelloStringGC {
    public static String[] array01 = new String[1000];
    public static String[] array02 = new String[1000];

    public static void main(String args[]) throws RuntimeException {
        String testString1 = "shared_test_string_unique_14325";
        String testString2 = "test123";

        WhiteBox wb = WhiteBox.getWhiteBox();
        if (!wb.isShared(testString1) && !wb.areSharedStringsIgnored()) {
            throw new RuntimeException("testString1 is not shared");
        }

        for (int i=0; i<5; i++) {
            allocSomeStrings(testString1, testString2);
            array01 = null;
            array02 = null;
            System.gc();
            sleep(300);
            array01 = new String[1000];
            array02 = new String[1000];
        }

        wb.fullGC();

        System.out.println("HelloStringGC: PASS");
    }

    private static void allocSomeStrings(String s1, String s2) {
        for (int i = 0; i < 1000; i ++) {
            array01[i] = new String(s1);
            array02[i] = new String(s2);
        }
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

}
