/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8262739
 * @summary Test correct insertion of anti-dependencies after String inflation.
 * @run main/othervm -Xbatch
 *                   compiler.controldependency.TestAntiDependencyAfterStringInflation
 */

package compiler.controldependency;

public class TestAntiDependencyAfterStringInflation {

    static String reverseString(String str) {
        int size = str.length();
        char[] buffer = new char[size];
        reverse(str, buffer, size);
        return new String(buffer, 0, size);
    }

    static void reverse(String str, char[] buffer, int size) {
        // Inflate String.value byte[] to char[]
        str.getChars(0, size, buffer, 0);
        // Reverse String by copying buffer elements. This will fail
        // if C2 does not insert anti-dependencies between loads/stores.
        int half = size / 2;
        for (int l = 0, r = size - 1; l < half; l++, r--) {
            char tmp = buffer[l];
            buffer[l] = buffer[r];
            buffer[r] = tmp;
        }
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 50_000; i++) {
            String res = reverseString("0123456789");
            if (!res.equals("9876543210")) {
                throw new RuntimeException("Unexpected result: " + res);
            }
        }
    }
}
