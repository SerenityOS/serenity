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
 * @run testng MethodReferenceTestSuperDefault
 */

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Robert Field
 */

interface DSPRI { String m(String a); }

interface DSPRA {
    default String xsA__(String s) {
        return "A__xsA:" + s;
    }

    default String xsAB_(String s) {
        return "AB_xsA:" + s;
    }

}

interface DSPRB extends DSPRA {

    default String xsAB_(String s) {
        return "AB_xsB:" + s;
    }

    default String xs_B_(String s) {
        return "_B_xsB:" + s;
    }

}

@Test
public class MethodReferenceTestSuperDefault implements DSPRB {

    public void testMethodReferenceSuper() {
        DSPRI q;

        q = DSPRB.super::xsA__;
        assertEquals(q.m("*"), "A__xsA:*");

        q = DSPRB.super::xsAB_;
        assertEquals(q.m("*"), "AB_xsB:*");

        q = DSPRB.super::xs_B_;
        assertEquals(q.m("*"), "_B_xsB:*");
    }

}
