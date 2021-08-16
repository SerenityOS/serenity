/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8027445
 * @summary String.equals() may be called with a length whose upper bits are not cleared
 *
 * @run main/othervm -XX:-UseOnStackReplacement -XX:-BackgroundCompilation
 *      compiler.intrinsics.string.TestStringEqualsBadLength
 */

package compiler.intrinsics.string;

import java.util.Arrays;

public class TestStringEqualsBadLength {

    int v1;
    int v2;

    boolean m(String s1) {
        int l = v2 - v1; // 0 - (-1) = 1. On 64 bit: 0xffffffff00000001
        char[] arr = new char[l];
        arr[0] = 'a';
        String s2 = new String(arr);
        // The string length is not reloaded but the value computed is
        // reused so pointer computation must not use
        // 0xffffffff00000001
        return s2.equals(s1);
    }

    // Same thing with String.compareTo()
    int m2(String s1) {
        int l = v2 - v1;
        char[] arr = new char[l+1];
        arr[0] = 'a';
        arr[1] = 'b';
        String s2 = new String(arr);
        return s2.compareTo(s1);
    }

    // Same thing with equals() for arrays
    boolean m3(char[] arr1) {
        int l = v2 - v1; // 0 - (-1) = 1. On 64 bit: 0xffffffff00000001
        char[] arr2 = new char[l];
        arr2[0] = 'a';
        return Arrays.equals(arr2, arr1);
    }

    static public void main(String[] args) {
        TestStringEqualsBadLength tse = new TestStringEqualsBadLength();
        tse.v1 = -1;
        tse.v2 = 0;
        char[] arr = new char[1];
        arr[0] = 'a';
        for (int i = 0; i < 20000; i++) {
            tse.m("a");
            tse.m2("ab");
            tse.m3(arr);
        }

        System.out.println("TEST PASSED");
    }
}
