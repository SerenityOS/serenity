/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8144212
 * @summary Check for correct memory flow with the String compress/inflate intrinsics.
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 *
 * @run main compiler.intrinsics.string.TestStringIntrinsicMemoryFlow
 */

package compiler.intrinsics.string;

import jdk.test.lib.Asserts;

public class TestStringIntrinsicMemoryFlow {

    public static void main(String[] args) {
        for (int i = 0; i < 100_000; ++i) {
            String s = "MyString";
            char[] c = {'M'};
            char res = testInflate1(s);
            Asserts.assertEquals(res, 'M', "testInflate1 failed");
            res = testInflate2(s);
            Asserts.assertEquals(res, (char)42, "testInflate2 failed");
            res = testCompress1(c);
            Asserts.assertEquals(res, 'M', "testCompress1 failed");
            byte resB = testCompress2(c);
            Asserts.assertEquals(resB, (byte)42, "testCompress2 failed");
        }
    }

    private static char testInflate1(String s) {
        char c[] = new char[1];
        // Inflate String from byte[] to char[]
        s.getChars(0, 1, c, 0);
        // Read char[] memory written by inflate intrinsic
        return c[0];
    }

    private static char testInflate2(String s) {
        char c1[] = new char[1];
        char c2[] = new char[1];
        c2[0] = 42;
        // Inflate String from byte[] to char[]
        s.getChars(0, 1, c1, 0);
        // Read char[] memory written before inflation
        return c2[0];
    }

    private static char testCompress1(char[] c) {
        // Compress String from char[] to byte[]
        String s = new String(c);
        // Read the memory written by compress intrinsic
        return s.charAt(0);
    }

    private static byte testCompress2(char[] c) {
        byte b1[] = new byte[1];
        b1[0] = 42;
        // Compress String from char[] to byte[]
        new String(c);
        // Read byte[] memory written before compression
        return b1[0];
    }
}
