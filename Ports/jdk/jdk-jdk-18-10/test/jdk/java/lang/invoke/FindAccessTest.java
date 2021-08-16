/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @bug 8139885
 * @run testng/othervm -ea -esa test.java.lang.invoke.FindAccessTest
 */

package test.java.lang.invoke;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

import static org.testng.AssertJUnit.*;

import org.testng.annotations.*;

/**
 * Tests for Lookup.findClass/accessClass extensions added in JEP 274.
 */
public class FindAccessTest {

    static final Lookup LOOKUP = MethodHandles.lookup();

    @Test
    public static void testFindSpecial() throws Throwable {
        FindSpecial.C c = new FindSpecial.C();
        assertEquals("I1.m", c.m());
        MethodType t = MethodType.methodType(String.class);
        MethodHandle ci1m = LOOKUP.findSpecial(FindSpecial.I1.class, "m", t, FindSpecial.C.class);
        assertEquals("I1.m", (String) ci1m.invoke(c));
    }

    @Test
    public static void testFindSpecialAbstract() throws Throwable {
        FindSpecial.C c = new FindSpecial.C();
        assertEquals("q", c.q());
        MethodType t = MethodType.methodType(String.class);
        boolean caught = false;
        try {
            MethodHandle ci3q = LOOKUP.findSpecial(FindSpecial.I3.class, "q", t, FindSpecial.C.class);
        } catch (Throwable thrown) {
            if (!(thrown instanceof IllegalAccessException) || !FindSpecial.ABSTRACT_ERROR.equals(thrown.getMessage())) {
                throw new AssertionError(thrown.getMessage(), thrown);
            }
            caught = true;
        }
        assertTrue(caught);
    }

    @Test(expectedExceptions = {ClassNotFoundException.class})
    public static void testFindClassCNFE() throws ClassNotFoundException, IllegalAccessException {
        LOOKUP.findClass("does.not.Exist");
    }

    static class FindSpecial {

        interface I1 {
            default String m() {
                return "I1.m";
            }
        }

        interface I2 {
            default String m() {
                return "I2.m";
            }
        }

        interface I3 {
            String q();
        }

        static class C implements I1, I2, I3 {
            public String m() {
                return I1.super.m();
            }
            public String q() {
                return "q";
            }
        }

        static final String ABSTRACT_ERROR = "no such method: test.java.lang.invoke.FindAccessTest$FindSpecial$I3.q()String/invokeSpecial";

    }

}
