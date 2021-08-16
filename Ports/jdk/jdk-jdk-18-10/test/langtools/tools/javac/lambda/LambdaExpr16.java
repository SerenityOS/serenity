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
 *  check that super inside lambda is handled correctly
 * @run main LambdaExpr16
 */
public class LambdaExpr16 {

    static int assertionCount = 0;

    static void assertTrue(boolean cond) {
        assertionCount++;
        if (!cond)
            throw new AssertionError();
    }

    interface A { void m(); }

    static class Sup {
       void m() {
          assertTrue(true);
       }
    }

    static class Sub extends Sup {
        void testLambda1() {
            A a = ()->{ super.m(); };
            a.m();
        }
        void testLambda2() {
            A a = () -> { A a1 = () -> { super.m(); }; a1.m(); };
            a.m();
        }
        void testRef1() {
            A a = () -> { A a1 = super::m; a1.m(); };
            a.m();
        }
        void testRef2() {
            A a = () -> { A a1 = () -> { A a2 = super::m; a2.m(); }; a1.m(); };
            a.m();
        }
    }

   public static void main(String[] args) {
      Sub s = new Sub();
      s.testLambda1();
      s.testLambda2();
      s.testRef1();
      s.testRef2();
      assertTrue(assertionCount == 4);
   }
}
