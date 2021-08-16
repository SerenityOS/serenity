/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8150804
 * @summary Tests elimination of Phi nodes without losing type information.
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *                   compiler.types.TestPhiElimination
 */

package compiler.types;

public class TestPhiElimination {
    /*
       A::get() is inlined into test(obj) producing the following graph:

               Parm (obj)
            TestPhiElimination
                   |
                 CastPP
        TestPhiElimination:NotNull
                   |
              CheckCastPP
               A:NotNull
               /       \
       CheckCastPP     |
        A:NotNull      |
                \     /
                  Phi
                   A
                   |
               Safepoint

       PhiNode::ideal() then replaces the Phi by a CheckCastPP:

               Parm (obj)
            TestPhiElimination
                   |
              CheckCastPP
                   A
                   |
               Safepoint

       losing the :NotNull information. Therefore, we cannot prove that obj != null
       when accessing a field and add an uncommon trap. Since obj is used as monitor, we
       set it to TOP in the uncommon trap branch and later fail in Process_OopMap_Node
       because the monitor object is TOP.
    */
    public Object test(TestPhiElimination obj) {
        if (obj instanceof A) {
            return ((A) obj).get();
        }
        return null;
    }

    static public void main(String[] args) {
        TestPhiElimination t = new TestPhiElimination();

        // Warmup
        B b = new B();
        for (int i = 0; i < 1_000; ++i) {
            t.test(b);
        }

        // Compile
        A a = new A();
        for (int i = 0; i < 20_000; ++i) {
            if (i % 2 == 0) {
                a.f = null;
            }
            t.test(a);
        }
    }

    static class A extends TestPhiElimination {
        public Object f;

        public A create() {
            return new A();
        }

        public synchronized Object get() {
            if (f == null) {
                f = create();
            }
            return f;
        }
    }

    static class B extends A { }
}
