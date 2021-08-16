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
 *  missing erasure on intersection supertype of generated lambda class
 */
public class TargetType35 {

    static int assertionCount = 0;

    static void assertTrue(boolean cond) {
        assertionCount++;
        if (!cond)
            throw new AssertionError();
    }

    interface A {}

    interface B {}

    static class C implements A, B {}

    static class D implements A, B {}

    interface SAM<Y, X> {
        Y invoke(X arg);
    }

    static class Sup {
       <Z> Z m(Z z) { return z; }
    }

    static class Sub extends Sup {
        <Z> Z m(Z z) { return z; }

        void test(C c, D d) {
            choose(c, d, x->x);
            choose(c, d, this::m);
            choose(c, d, super::m);
        }

        <T> void choose(T t1, T t2, SAM<T, T> t3) {
            assertTrue(true);
        }
    }

    public static void main(String[] args)
    {
        new Sub().test(null, null);
        assertTrue(assertionCount == 3);
    }
}
