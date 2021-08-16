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

/**
 * @test
 * @bug 6478991
 * @summary C1 NullCheckEliminator yields incorrect exceptions
 *
 * @run main/othervm -Xcomp
 *      -XX:CompileCommand=compileonly,compiler.c1.NullCheckTest::test
 *      -XX:CompileCommand=compileonly,compiler.c1.NullCheckTest::inlined
 *      compiler.c1.NullCheckTest
 */

package compiler.c1;

public class NullCheckTest {
    static class A {
        int f;

        public final void inlined(A a) {
            // This cast is intended to fail.
            B b = ((B) a);
        }
    }

    static class B extends A {
    }


    private static void test(A a1, A a2) {
        // Inlined call must do a null check on a1.
        // However, the exlipcit NullCheck instruction is eliminated and
        // the null check is folded into the field load below, so the
        // exception in the inlined method is thrown before the null check
        // and the NullPointerException is not thrown.
        a1.inlined(a2);

        int x = a1.f;
    }

    public static void main(String[] args) {
        // load classes
        new B();
        try {
                test(null, new A());

                throw new InternalError("FAILURE: no exception");
        } catch (NullPointerException ex) {
                System.out.println("CORRECT: NullPointerException");
        } catch (ClassCastException ex) {
                System.out.println("FAILURE: ClassCastException");
                throw ex;
        }
    }
}
