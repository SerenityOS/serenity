/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6979683
 * @summary Verify that casts can narrow and unbox at the same time
 * @author jrose
 *
 * @compile TestCast6979683_GOOD.java
 * @run main TestCast6979683_GOOD
 */

public class TestCast6979683_GOOD {
    public static void main(String... av) {
        bugReportExample();
        for (int x = -1; x <= 2; x++) {
            zconvTests(x != 0);
            iconvTests(x);
            bconvTests((byte)x);
            cconvTests((char)x);
        }
        System.out.println("Successfully ran "+tests+" tests.");
    }

    static int tests;
    static void assertEquals(Object x, Object y) {
        if (!x.equals(y)) {
            throw new RuntimeException("assertEquals: "+x+" != "+y);
        }
        ++tests;
    }

    static void bugReportExample() {
  {} // example in bug report:
  Object x = (Object)1;
  int y = (int)x;
  {} // end example
    }

    static boolean zconv1(Boolean o) { return o; }
    static boolean zconv2(Object o) { return (boolean)o; }
    static boolean zconv3(Comparable<Boolean> o) { return (boolean)o; }

    static void zconvTests(boolean x) {
        assertEquals(x, zconv1(x));
        assertEquals(x, zconv2(x));
        assertEquals(x, zconv3(x));
    }

    static int iconv1(Integer o) { return o; }
    static int iconv2(Object o) { return (int)o; }
    static int iconv3(java.io.Serializable o) { return (int)o; }
    static int iconv4(Number o) { return (int)o; }
    static int iconv5(Comparable<Integer> o) { return (int)o; }

    static void iconvTests(int x) {
        assertEquals(x, iconv1(x));
        assertEquals(x, iconv2(x));
        assertEquals(x, iconv3(x));
        assertEquals(x, iconv4(x));
        assertEquals(x, iconv5(x));
    }

    static float bconv1(Byte o) { return o; }  // note type "float"
    static float bconv2(Object o) { return (byte)o; }
    static float bconv3(java.io.Serializable o) { return (byte)o; }
    static float bconv4(Number o) { return (byte)o; }

    static void bconvTests(byte x) {
        float xf = x;
        assertEquals(xf, bconv1(x));
        assertEquals(xf, bconv2(x));
        assertEquals(xf, bconv3(x));
        assertEquals(xf, bconv4(x));
    }

    static float cconv1(Character o) { return o; }  // note type "float"
    static float cconv2(Object o) { return (char)o; }
    static float cconv3(java.io.Serializable o) { return (char)o; }
    static float cconv4(Comparable<Character> o) { return (char)o; }

    static void cconvTests(char x) {
        float xf = x;
        assertEquals(xf, cconv1(x));
        assertEquals(xf, cconv2(x));
        assertEquals(xf, cconv3(x));
        assertEquals(xf, cconv4(x));
    }

}
