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

public class InternStringTest {
    public static String passed_output1 = "Found shared string.";
    public static String passed_output2 = "Shared strings are equal.";
    public static String passed_output3 = "Found shared string containing latin1 supplement chars.";
    public static String passed_output4 = "Found shared string containing non-western chars.";
    public static final String latin1Sup  = "XXXX \u00a3 YYYY"; // \u00a3 = The pound sign
    public static final String nonWestern = "XXXX \u5678 YYYY"; // \u5678 = Unicode Han Character 'ton (metric or English)'

    public static void main(String[] args) throws Exception {
        WhiteBox wb = WhiteBox.getWhiteBox();

        // All string literals are shared.
        String shared1 = "LiveOak";
        String interned1 = shared1.intern();
        if (wb.areSharedStringsIgnored() || wb.isShared(interned1)) {
            System.out.println(passed_output1);
        } else {
            throw new RuntimeException("Failed: String is not shared.");
        }

        // Test 2: shared_string1.intern() == shared_string2.intern()
        String shared2 = "LiveOak";
        String interned2 = shared2.intern();
        if (interned1 == interned2) {
            System.out.println(passed_output2);
        } else {
            throw new RuntimeException("Not equal!");
        }

        // Test 3: interned strings with a char in latin1 supplement block [\u0080-\u00ff]
        {
            String a = "X" + latin1Sup.substring(1);
            String b = a.intern();

            if (wb.areSharedStringsIgnored() || wb.isShared(b)) {
                System.out.println(passed_output3);
            } else {
                throw new RuntimeException("Failed: expected shared string with latin1-supplement chars.");
            }
        }

        // Test 5: interned strings with non-western characters
        {
            String a = "X" + nonWestern.substring(1);
            String b = a.intern();
            if (wb.areSharedStringsIgnored() || wb.isShared(b)) {
                System.out.println(passed_output4);
            } else {
                throw new RuntimeException("Failed: expected shared string with non-western chars.");
            }
        }
    }
}
