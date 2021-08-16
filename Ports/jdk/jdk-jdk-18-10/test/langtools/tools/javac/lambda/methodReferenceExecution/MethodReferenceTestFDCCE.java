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
 * @run testng MethodReferenceTestFDCCE
 */

import org.testng.annotations.Test;
import java.lang.reflect.Array;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Method references and raw types.
 * @author Robert Field
 */

@Test
@SuppressWarnings({"rawtypes", "unchecked"})
public class MethodReferenceTestFDCCE {

    static void assertCCE(Throwable t) {
        assertEquals(t.getClass().getName(), "java.lang.ClassCastException");
    }

    interface Pred<T> { boolean accept(T x); }

    interface Ps { boolean accept(short x); }

    interface Oo { Object too(int x); }

    interface Reto<T> { T m(); }

    class A {}
    class B extends A {}

    static boolean isMinor(int x) {
        return x < 18;
    }

    static boolean tst(A x) {
        return true;
    }

    static Object otst(Object x) {
        return x;
    }

    static boolean stst(Short x) {
        return x < 18;
    }

    static short ritst() {
        return 123;
    }

    public void testMethodReferenceFDPrim1() {
        Pred<Byte> p = MethodReferenceTestFDCCE::isMinor;
        Pred p2 = p;
        assertTrue(p2.accept((Byte)(byte)15));
    }

    public void testMethodReferenceFDPrim2() {
        Pred<Byte> p = MethodReferenceTestFDCCE::isMinor;
        Pred p2 = p;
        assertTrue(p2.accept((byte)15));
    }

    public void testMethodReferenceFDPrimICCE() {
        Pred<Byte> p = MethodReferenceTestFDCCE::isMinor;
        Pred p2 = p;
        try {
            p2.accept(15); // should throw CCE
            fail("Exception should have been thrown");
        } catch (Throwable t) {
            assertCCE(t);
        }
    }

    public void testMethodReferenceFDPrimOCCE() {
        Pred<Byte> p = MethodReferenceTestFDCCE::isMinor;
        Pred p2 = p;
        try {
            p2.accept(new Object()); // should throw CCE
            fail("Exception should have been thrown");
        } catch (Throwable t) {
            assertCCE(t);
        }
    }

    public void testMethodReferenceFDRef() {
        Pred<B> p = MethodReferenceTestFDCCE::tst;
        Pred p2 = p;
        assertTrue(p2.accept(new B()));
    }

    public void testMethodReferenceFDRefCCE() {
        Pred<B> p = MethodReferenceTestFDCCE::tst;
        Pred p2 = p;
        try {
            p2.accept(new A()); // should throw CCE
            fail("Exception should have been thrown");
        } catch (Throwable t) {
            assertCCE(t);
        }
    }

    public void testMethodReferenceFDPrimPrim() {
        Ps p = MethodReferenceTestFDCCE::isMinor;
        assertTrue(p.accept((byte)15));
    }

    public void testMethodReferenceFDPrimBoxed() {
        Ps p = MethodReferenceTestFDCCE::stst;
        assertTrue(p.accept((byte)15));
    }

    public void testMethodReferenceFDPrimRef() {
        Oo p = MethodReferenceTestFDCCE::otst;
        assertEquals(p.too(15).getClass().getName(), "java.lang.Integer");
    }

    public void testMethodReferenceFDRet1() {
        Reto<Short> p = MethodReferenceTestFDCCE::ritst;
        assertEquals(p.m(), (Short)(short)123);
    }
}
