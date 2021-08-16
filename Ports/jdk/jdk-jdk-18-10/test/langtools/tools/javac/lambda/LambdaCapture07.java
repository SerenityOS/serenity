/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8004962
 * @summary Code generation crash with lambda and local classes
 */
public class LambdaCapture07 {

    static int assertionCount = 0;

    static void assertTrue(boolean cond) {
        assertionCount++;
        if (!cond)
            throw new AssertionError();
    }

    interface SAM {
        void m();
    }

    void test(int i) {
        class Local { Local() { assertTrue(i == 42); } }
        class LocalSub extends Local { }
        SAM s_sup = ()->new Local();
        s_sup.m();
        SAM s_sub = ()->new LocalSub();
        s_sub.m();
        SAM s_sup_nested = ()->{ SAM s = ()->new Local(); s.m(); };
        s_sup_nested.m();
        SAM s_sub_nested = ()->{ SAM s = ()->new LocalSub(); s.m(); };
        s_sub_nested.m();
    }

    public static void main(String[] args) {
        new LambdaCapture07().test(42);
        assertTrue(assertionCount == 4);
    }
}
