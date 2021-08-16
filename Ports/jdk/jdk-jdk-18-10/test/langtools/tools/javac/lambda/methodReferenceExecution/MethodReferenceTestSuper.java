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
 * @run testng MethodReferenceTestSuper
 */

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Robert Field
 */

interface SPRI { String m(String a); }

class SPRA {
    String xsA__(String s) {
        return "A__xsA:" + s;
    }

    String xsA_M(String s) {
        return "A_MxsA:" + s;
    }

    String xsAB_(String s) {
        return "AB_xsA:" + s;
    }

    String xsABM(String s) {
        return "ABMxsA:" + s;
    }

}

class SPRB extends SPRA {

    String xsAB_(String s) {
        return "AB_xsB:" + s;
    }

    String xsABM(String s) {
        return "ABMxsB:" + s;
    }

    String xs_B_(String s) {
        return "_B_xsB:" + s;
    }

    String xs_BM(String s) {
        return "_BMxsB:" + s;
    }

}

@Test
public class MethodReferenceTestSuper extends SPRB {

    String xsA_M(String s) {
        return "A_MxsM:" + s;
    }


    String xsABM(String s) {
        return "ABMxsM:" + s;
    }

    String xs_BM(String s) {
        return "_BMxsM:" + s;
    }

    public void testMethodReferenceSuper() {
        SPRI q;

        q = super::xsA__;
        assertEquals(q.m("*"), "A__xsA:*");

        q = super::xsA_M;
        assertEquals(q.m("*"), "A_MxsA:*");

        q = super::xsAB_;
        assertEquals(q.m("*"), "AB_xsB:*");

        q = super::xsABM;
        assertEquals(q.m("*"), "ABMxsB:*");

        q = super::xs_B_;
        assertEquals(q.m("*"), "_B_xsB:*");

        q = super::xs_BM;
        assertEquals(q.m("*"), "_BMxsB:*");
    }

}
