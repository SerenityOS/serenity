/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 *  check that method reference handles varargs conversion properly
 * @run main MethodReference36
 */

public class MethodReference36 {

    static int assertionCount = 0;

    static void assertTrue(boolean cond) {
        assertionCount++;
        if (!cond)
            throw new AssertionError();
    }

    interface SamC { void m(char[] a); }
    interface SamZ { void m(boolean[] a); }
    interface SamB { void m(byte[] a); }
    interface SamS { void m(short[] a); }
    interface SamI { void m(int[] a); }
    interface SamL { void m(long[] a); }
    interface SamF { void m(float[] a); }
    interface SamD { void m(double[] a); }
    interface SamO { void m(Object[] a); }


    static void m(Object... vi) {
        assertTrue(true);
    }

    public void test() {

        SamC sc = MethodReference36::m;
        sc.m(new char[] { 'a', 'b' } );

        SamZ sz = MethodReference36::m;
        sz.m(new boolean[] { true, false } );

        SamB sb = MethodReference36::m;
        sb.m(new byte[] { 0, 1 } );

        SamS ss = MethodReference36::m;
        ss.m(new short[] { 0, 1 } );

        SamI si = MethodReference36::m;
        si.m(new int[] { 0, 1 } );

        SamL sl = MethodReference36::m;
        sl.m(new long[] { 0, 1 } );

        SamF sf = MethodReference36::m;
        sf.m(new float[] { 0, 1 } );

        SamD sd = MethodReference36::m;
        sd.m(new double[] { 0, 1 } );

        SamO so = MethodReference36::m;
        so.m(new Object[] { null, null } );
    }

    public static void main(String[] args) {
       new MethodReference36().test();
       assertTrue(assertionCount == 9);
    }
}
