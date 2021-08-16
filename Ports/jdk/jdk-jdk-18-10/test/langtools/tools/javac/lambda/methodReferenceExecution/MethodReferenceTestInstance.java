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
 * @run testng MethodReferenceTestInstance
 */

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Robert Field
 */

class MethodReferenceTestInstance_E {
    String xI(Integer i) {
        return "ExI:" + i;
    }
}

@Test
public class MethodReferenceTestInstance {

    interface SI { String m(Integer a); }

    String xI(Integer i) {
        return "xI:" + i;
    }

    public void testMethodReferenceInstance() {
        SI q;

        q = this::xI;
        assertEquals(q.m(55), "xI:55");
    }

    public void testMethodReferenceExternal() {
        SI q;

        q = (new MethodReferenceTestInstance_E())::xI;
        assertEquals(q.m(77), "ExI:77");
    }

}
