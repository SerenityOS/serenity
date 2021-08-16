/*
 * Copyright (c) 1998, 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4098316 4522720
 * @summary Test chained assignments using access methods.
 * @author William Maddox (maddox)
 *
 * @compile ChainedAssignment.java
 * @run main ChainedAssignment
 */

public class ChainedAssignment {

    private int a = 0;
    private int b = 0;
    private int c = 0;

    static private int sa = 0;
    static private int sb = 0;
    static private int sc = 0;

    private class Inner {

        void test1() throws Exception {
            (a) = (b) = 1;
            if (a != 1 || b != 1) {
                throw new Exception("FAILED (11)");
            }
            System.out.println(a + " " + b + " " + c);
            a = b = c;
            if (a != 0 || b != 0) {
                throw new Exception("FAILED (12)");
            }
            System.out.println(a + " " + b + " " + c);
            a = (b) += 5;
            if (a != 5 || b != 5) {
                throw new Exception("FAILED (13)");
            }
            System.out.println(a + " " + b + " " + c);
        }

        void test2() throws Exception {
            sa = sb = 1;
            if (sa != 1 || sb != 1) {
                throw new Exception("FAILED (21)");
            }
            System.out.println(sa + " " + sb + " " + sc);
            sa = sb = sc;
            if (sa != 0 || sb != 0) {
                throw new Exception("FAILED (22)");
            }
            System.out.println(sa + " " + sb + " " + sc);
            sa = sb += 5;
            if (sa != 5 || sb != 5) {
                throw new Exception("FAILED (23)");
            }
            System.out.println(sa + " " + sb + " " + sc);
        }

    }

    public static void main(String[] args) throws Exception {
        ChainedAssignment outer = new ChainedAssignment();
        Inner inner = outer.new Inner();
        inner.test1();
        inner.test2();
    }

}
