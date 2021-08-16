/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @summary Unit test for buffers
 * @bug 4413135 4414911 4416536 4416562 4418782 4471053 4472779 4490253 4523725
 *      4526177 4463011 4660660 4661219 4663521 4782970 4804304 4938424 5029431
 *      5071718 6231529 6221101 6234263 6535542 6591971 6593946 6795561 7190219
 *      7199551 8065556 8149469 8230665 8237514
 * @modules java.base/java.nio:open
 *          java.base/jdk.internal.misc
 * @author Mark Reinhold
 */


import java.io.PrintStream;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;


public class Basic {

    static PrintStream out = System.err;

    static long ic(int i) {
        int j = i % 54;
        return j + 'a' + ((j > 26) ? 128 : 0);
    }

    static String toString(Buffer b) {
        return (b.getClass().getName()
                + "[pos=" + b.position()
                + " lim=" + b.limit()
                + " cap=" + b.capacity()
                + "]");
    }

    static void show(int level, Buffer b) {
        for (int i = 0; i < level; i++)
            out.print("  ");
        out.println(toString(b) + " " + Integer.toHexString(b.hashCode()));
    }

    static void fail(String s) {
        throw new RuntimeException(s);
    }

    static void fail(String s, Buffer b) {
        throw new RuntimeException(s + ": " + toString(b));
    }

    static void fail(String s, Buffer b, Buffer b2) {
        throw new RuntimeException(s + ": "
                                   + toString(b) + ", " + toString(b2));
    }

    static void fail(Buffer b,
                     String expected, char expectedChar,
                     String got, char gotChar)
    {
        if (b instanceof ByteBuffer) {
            ByteBuffer bb = (ByteBuffer)b;
            int n = Math.min(16, bb.limit());
            for (int i = 0; i < n; i++)
                out.print(" " + Integer.toHexString(bb.get(i) & 0xff));
            out.println();
        }
        if (b instanceof CharBuffer) {
            CharBuffer bb = (CharBuffer)b;
            int n = Math.min(16, bb.limit());
            for (int i = 0; i < n; i++)
                out.print(" " + Integer.toHexString(bb.get(i) & 0xffff));
            out.println();
        }
        throw new RuntimeException(toString(b)
                                   + ": Expected '" + expectedChar + "'=0x"
                                   + expected
                                   + ", got '" + gotChar + "'=0x"
                                   + got);
    }

    static void fail(Buffer b, long expected, long got) {
        fail(b,
             Long.toHexString(expected), (char)expected,
             Long.toHexString(got), (char)got);
    }

    static void ck(Buffer b, boolean cond) {
        if (!cond)
            fail("Condition failed", b);
    }

    static void ck(Buffer b, long got, long expected) {
        if (expected != got)
            fail(b, expected, got);
    }

    static void ck(Buffer b, float got, float expected) {
        if (expected != got)
            fail(b,
                 Float.toString(expected), (char)expected,
                 Float.toString(got), (char)got);
    }

    static void ck(Buffer b, double got, double expected) {
        if (expected != got)
            fail(b,
                 Double.toString(expected), (char)expected,
                 Double.toString(got), (char)got);
    }

    public static void main(String[] args) {
        BasicByte.test();
        BasicChar.test();
        BasicShort.test();
        BasicInt.test();
        BasicLong.test();
        BasicFloat.test();
        BasicDouble.test();
    }

}
