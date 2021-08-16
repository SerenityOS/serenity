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

/*
 * @test
 * @key randomness
 * @bug 8267332
 * @summary Test meet on xor
 * @library /test/lib /
 * @run main/othervm compiler.types.TestMeetXor -Xbatch -XX::CompileCommand=dontinline,*::test*
 */

package compiler.types;

import java.util.Random;
import jdk.test.lib.Asserts;

public class TestMeetXor {
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 50_000; i++) {
            testCase1E();
            testCase2E();
            testCase3E();
            testCase4E();

            testCaseS();
        }
    }

    static int[] count = new int[256];
    static Random r = jdk.test.lib.Utils.getRandomInstance();

    public static void testCase1E() {
        boolean aiobe = false;
        try {
            testBound1E(r.nextInt());
        } catch (ArrayIndexOutOfBoundsException e) {
            aiobe = true;
        }
        Asserts.assertTrue(aiobe);
    }

    public static void testBound1E(int i1) throws ArrayIndexOutOfBoundsException {
        int index = (i1 >>> 24) ^ -1;
        count[index]++;
    }

    public static void testCase2E() {
        boolean aiobe = false;
        try {
            testBound2E(r.nextInt());
        } catch (ArrayIndexOutOfBoundsException e) {
            aiobe = true;
        }
        Asserts.assertTrue(aiobe);
    }

    public static void testBound2E(int i1) throws ArrayIndexOutOfBoundsException {
        int index = (i1 >>> 24) ^ 0x100;
        count[index]++;
    }

    public static void testCase3E() {
        boolean aiobe = false;
        try {
            testBound3E(r.nextInt());
        } catch (ArrayIndexOutOfBoundsException e) {
            aiobe = true;
        }
        Asserts.assertTrue(aiobe);
    }

    public static void testBound3E(int i1) throws ArrayIndexOutOfBoundsException {
        int index = (i1 >>> 24) ^ Integer.MIN_VALUE;
        count[index]++;
    }

    public static void testCase4E() {
        boolean aiobe = false;
        try {
            testBound4E(r.nextInt(), 0xf0f0ff);
        } catch (ArrayIndexOutOfBoundsException e) {
            aiobe = true;
        }
        Asserts.assertTrue(aiobe);
    }

    public static void testBound4E(int i1, int i2) throws ArrayIndexOutOfBoundsException {
        int index = (i1 >>> 24) ^ i2;
        count[index]++;
    }

    public static void testCaseS() {
        testBound1S(r.nextInt());
        testBound2S(r.nextInt());
        testBound3S(r.nextInt(), r.nextInt());
        testBound4S(r.nextInt());
    }

    public static void testBound1S(int i1) throws ArrayIndexOutOfBoundsException {
        int index = (i1 >>> 24) ^ 0x80;
        count[index]++;
    }

    public static void testBound2S(int i1) throws ArrayIndexOutOfBoundsException {
        int index = (i1 >>> 24) ^ 0xff;
        count[index]++;
    }

    public static void testBound3S(int i1, int i2) throws ArrayIndexOutOfBoundsException {
        int index = (i1 >>> 24) ^ (i2 >>> 24);
        count[index]++;
    }

    public static void testBound4S(int i1) throws ArrayIndexOutOfBoundsException {
        int index = (i1 >>> 24) ^ 0;
        count[index]++;
    }
}
