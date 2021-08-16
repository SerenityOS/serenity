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
 * @run testng MethodReferenceTestVarArgsExt
 */

import org.testng.annotations.Test;
import java.lang.reflect.Array;

import static org.testng.Assert.assertEquals;

/**
 * @author Robert Field
 */

interface NXII { String m(Integer a, Integer b); }

interface NXiii { String m(int a, int b, int c); }

interface NXi { String m(int a); }

interface NXaO { String m(Object[] a); }

interface NXai { String m(int[] a); }

interface NXvi { String m(int... va); }

@Test
public class MethodReferenceTestVarArgsExt {

    // These should be processed as var args

    public void testVarArgsNXSuperclass() {
        NXII q;

        q = (new Ext())::xvO;
        assertEquals(q.m(55,66), "xvO:55*66*");
    }

    public void testVarArgsNXArray() {
        NXai q;

        q = (new Ext())::xvO;
        assertEquals(q.m(new int[] { 55,66 } ), "xvO:[55,66,]*");
    }

    public void testVarArgsNXII() {
        NXII q;

        q = (new Ext())::xvI;
        assertEquals(q.m(33,7), "xvI:33-7-");

        q = (new Ext())::xIvI;
        assertEquals(q.m(50,40), "xIvI:5040-");

        q = (new Ext())::xvi;
        assertEquals(q.m(100,23), "xvi:123");

        q = (new Ext())::xIvi;
        assertEquals(q.m(9,21), "xIvi:(9)21");
    }

    public void testVarArgsNXiii() {
        NXiii q;

        q = (new Ext())::xvI;
        assertEquals(q.m(3, 2, 1), "xvI:3-2-1-");

        q = (new Ext())::xIvI;
        assertEquals(q.m(888, 99, 2), "xIvI:88899-2-");

        q = (new Ext())::xvi;
        assertEquals(q.m(900,80,7), "xvi:987");

        q = (new Ext())::xIvi;
        assertEquals(q.m(333,27, 72), "xIvi:(333)99");
    }

    public void testVarArgsNXi() {
        NXi q;

        q = (new Ext())::xvI;
        assertEquals(q.m(3), "xvI:3-");

        q = (new Ext())::xIvI;
        assertEquals(q.m(888), "xIvI:888");

        q = (new Ext())::xvi;
        assertEquals(q.m(900), "xvi:900");

        q = (new Ext())::xIvi;
        assertEquals(q.m(333), "xIvi:(333)0");
    }

    // These should NOT be processed as var args

    public void testVarArgsNXaO() {
        NXaO q;

        q = (new Ext())::xvO;
        assertEquals(q.m(new String[] { "yo", "there", "dude" }), "xvO:yo*there*dude*");
    }


}

class Ext {

    String xvI(Integer... vi) {
        StringBuilder sb = new StringBuilder("xvI:");
        for (Integer i : vi) {
            sb.append(i);
            sb.append("-");
        }
        return sb.toString();
    }

    String xIvI(Integer f, Integer... vi) {
        StringBuilder sb = new StringBuilder("xIvI:");
        sb.append(f);
        for (Integer i : vi) {
            sb.append(i);
            sb.append("-");
        }
        return sb.toString();
    }

    String xvi(int... vi) {
        int sum = 0;
        for (int i : vi) {
            sum += i;
        }
        return "xvi:" + sum;
    }

    String xIvi(Integer f, int... vi) {
        int sum = 0;
        for (int i : vi) {
            sum += i;
        }
        return "xIvi:(" + f + ")" + sum;
    }

    String xvO(Object... vi) {
        StringBuilder sb = new StringBuilder("xvO:");
        for (Object i : vi) {
            if (i.getClass().isArray()) {
                sb.append("[");
                int len = Array.getLength(i);
                for (int x = 0; x < len; ++x)  {
                    sb.append(Array.get(i, x));
                    sb.append(",");
                }
                sb.append("]");

            } else {
                sb.append(i);
            }
            sb.append("*");
        }
        return sb.toString();
    }


}
