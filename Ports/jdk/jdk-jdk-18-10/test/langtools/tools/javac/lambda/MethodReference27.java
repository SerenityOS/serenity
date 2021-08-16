/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003280
 * @summary Add lambda tests
 *  check that non-boxing method references conversion has the precedence
 * @run main MethodReference27
 */

public class MethodReference27 {

    static void assertTrue(boolean cond) {
        assertionCount++;
        if (!cond)
            throw new AssertionError();
    }

    static int assertionCount = 0;

    interface SAM {
        void m(int i1, int i2);
    }

    static void m1(int i1, int i2) { assertTrue(true); }
    static void m1(Integer i1, int i2) { assertTrue(false); }
    static void m1(int i1, Integer i2) { assertTrue(false); }
    static void m1(Integer i1, Integer i2) { assertTrue(false); }
    static void m1(Integer... is) { assertTrue(false); }

    static void m2(int... is) { assertTrue(true); }
    static void m2(double... ds) { assertTrue(false); }

    public static void main(String[] args) {
        SAM s1 = MethodReference27::m1;
        s1.m(42,42);
        SAM s2 = MethodReference27::m2;
        s2.m(42,42);
        assertTrue(assertionCount == 2);
    }
}
