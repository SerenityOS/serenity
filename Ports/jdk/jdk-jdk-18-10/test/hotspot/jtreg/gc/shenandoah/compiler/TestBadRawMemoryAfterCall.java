/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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
 * @bug 8258393 8263376
 * @summary Shenandoah: "graph should be schedulable" assert failure
 * @requires vm.flavor == "server"
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm -XX:+UseShenandoahGC -XX:-BackgroundCompilation TestBadRawMemoryAfterCall
 *
 */

public class TestBadRawMemoryAfterCall {
    public static void main(String[] args) {
        A a = new A();
        B b = new B();
        C c = new C();
        for (int i = 0; i < 20_000; i++) {
            test1(a);
            test1(b);
            test1(c);

            test2(a, i);
            test2(b, i);
            test2(c, i);
        }
    }

    private static Object test1(A a) {
        if (a.getClass() == A.class) {
        }

        Object o = null;
        try {
            a.m();
            o = a.getClass();
        } catch (Exception e) {

        }
        return o;
    }

    static int field;

    private static Object test2(A a, int i) {
        if (a.getClass() == A.class) {
        }

        Object o = null;
        try {
            a.m();
            o = a.getClass();
        } catch (Exception e) {
            i = 42;
        }
        if (i == 42) {
            field = 42;
        }
        return o;
    }

    private static class A {
        void m() throws Exception {
            throw new Exception();
        }
    }
    private static class B extends A {
        void m() {
        }
    }
    private static class C extends B {
        void m() {
        }
    }
}
