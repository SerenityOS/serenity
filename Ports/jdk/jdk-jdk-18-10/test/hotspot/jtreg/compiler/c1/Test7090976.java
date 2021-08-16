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
 * @bug 7090976
 * @summary Eclipse/CDT causes a JVM crash while indexing C++ code
 *
 * @run main/othervm -XX:-BackgroundCompilation -XX:-UseOnStackReplacement
 *      compiler.c1.Test7090976
 */

package compiler.c1;

public class Test7090976 {

    static interface I1 {
        public void m1();
    };

    static interface I2 {
        public void m2();
    };

    static interface I extends I1,I2 {
    }

    static class A implements I1 {
        int v = 0;
        int v2;

        public void m1() {
            v2 = v;
        }
    }

    static class B implements I2 {
        Object v = new Object();
        Object v2;

        public void m2() {
            v2 = v;
        }
    }

    private void test(A a)
    {
        if (a instanceof I) {
            I i = (I)a;
            i.m1();
            i.m2();
        }
    }

    public static void main(String[] args)
    {
        Test7090976 t = new Test7090976();
        A a = new A();
        B b = new B();
        for (int i = 0; i < 10000; i++) {
            t.test(a);
        }
    }
}
