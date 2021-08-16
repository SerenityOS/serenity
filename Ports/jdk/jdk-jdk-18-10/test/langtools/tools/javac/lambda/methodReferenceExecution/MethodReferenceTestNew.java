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

/**
 * @test
 * @bug 8003639
 * @summary convert lambda testng tests to jtreg and add them
 * @run testng MethodReferenceTestNew
 */

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Robert Field
 */

@Test
public class MethodReferenceTestNew {

    interface M0<T> {

        T m();
    }

    static class N0 {

        N0() {
        }
    }

    interface M1<T> {

        T m(Integer a);
    }

    static class N1 {

        int i;

        N1(int i) {
            this.i = i;
        }
    }

    interface M2<T> {

        T m(Integer n, String o);
    }

    static class N2 {

        Number n;
        Object o;

        N2(Number n, Object o) {
            this.n = n;
            this.o = o;
        }

        public String toString() {
            return "N2(" + n + "," + o + ")";
        }
    }

    interface MV {

        NV m(Integer ai, int i);
    }

    static class NV {

        int i;

        NV(int... v) {
            i = 0;
            for (int x : v) {
                i += x;
            }
        }

        public String toString() {
            return "NV(" + i + ")";
        }
    }

    public void testConstructorReference0() {
        M0<N0> q;

        q = N0::new;
        assertEquals(q.m().getClass().getSimpleName(), "N0");
    }

    public void testConstructorReference1() {
        M1<N1> q;

        q = N1::new;
        assertEquals(q.m(14).getClass().getSimpleName(), "N1");
    }

    public void testConstructorReference2() {
        M2<N2> q;

        q = N2::new;
        assertEquals(q.m(7, "hi").toString(), "N2(7,hi)");
    }

    public void testConstructorReferenceVarArgs() {
        MV q;

        q = NV::new;
        assertEquals(q.m(5, 45).toString(), "NV(50)");
    }

}
