/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @requires vm.flavor == "server"
 *
 * @run main/othervm -Xbatch -XX:-TieredCompilation -XX:+PrintCompilation -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining
 *                   -XX:CompileCommand=quiet -XX:CompileCommand=compileonly,*::test
 *                     compiler.c2.UpcastTest
 */

package compiler.c2;

public class UpcastTest {
    static class Test1 {
        interface I { void m(); }

        static abstract class AC implements I {
            public void m() {}
        }

        static class T extends AC {
            int i = 0;

            static {
                // Run the test while T is not fully initialized yet.
                for (int i = 0; i < 20_000; i++) {
                    test(new T(){});
                    test(new T(){});
                    test(new T(){});
                }
            }
        }

        static void test(T t) {
            // Interface I has unique implementor AC.
            // CHA reports AC::m and the callee is guarded by a type check against its holder (AC).

            ((I)t).m(); // invokeinterface I.m w/ polluted profile

            // No upcasts (to class AC) on t after the call.
            // Otherwise, field access fires an assert.
            t.i = 1;
        }
    }

    static class Test2 {
        interface I { void m(); }
        interface J extends I {
            default void m() {}
        }

        static abstract class AC implements I {
        }

        static abstract class T extends AC {
            int i = 0;

            static {
                // Run the test while T is not fully initialized yet.
                for (int i = 0; i < 20_000; i++) {
                    test(new T1(){});
                    test(new T2(){});
                    test(new T3(){});
                }
            }
        }

        static class T1 extends T implements J {}
        static class T2 extends T implements J {}
        static class T3 extends T implements J {}

        static void test(T t) {
            // Interface I has unique implementor AC.
            // CHA reports J::m and the callee is guarded by a type check against its holder (J).

            ((I)t).m(); // invokeinterface I.m w/ polluted profile

            // No upcasts (to interface J) on t after the call.
            // Otherwise, field access fires an assert.
            t.i = 1;
        }
    }

    static class Test3 {
        interface I {
            default void m1() { m2(); }
            void m2();
        }
        interface J extends I {
            default void m2() {}
        }

        static abstract class AC implements I {}

        static class T extends AC implements J {
            int i = 0;
        }

        static void test(T t) {
            t.m1(); // invokeinterface I.m w/ polluted profile

            // No upcasts (to interface J) on t after the call.
            // Otherwise, field access fires an assert.
            t.i = 1;
        }

        static void run() {
            for (int i = 0; i < 20_000; i++) {
                test(new T() {});
                test(new T() {});
                test(new T() {});
            }
        }
    }

    static class Test4 {
        interface I { default void m() {}}

        static class T {
            int i = 0;
        }

        static class D extends T implements I {}

        static void test(T t) {
            if (t instanceof I) {
                ((I)t).m();

                // No upcasts (to interface J) on t after the call.
                // Otherwise, field access fires an assert.

                t.i = 1;
            } else {
                throw new InternalError();
            }
        }

        static void run() {
            for (int i = 0; i < 20_000; i++) {
                test(new D() {});
                test(new D() {});
                test(new D() {});
            }
        }
    }

    public static void main(String[] args) {
        new Test1.T();  // trigger initialization of class T
        new Test2.T1(); // trigger initialization of class T
        Test3.run();
        Test4.run();
    }
}
