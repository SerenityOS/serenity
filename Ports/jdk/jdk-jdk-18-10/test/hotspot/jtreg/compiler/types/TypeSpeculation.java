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
 * @bug 8024070
 * @summary Test that type speculation doesn't cause incorrect execution
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:-UseOnStackReplacement
 *                   -XX:-BackgroundCompilation -XX:TypeProfileLevel=222
 *                   -XX:+UseTypeSpeculation
 *                   compiler.types.TypeSpeculation
 */

package compiler.types;

public class TypeSpeculation {

    interface I {
    }

    static class A {
        int m() {
            return 1;
        }
    }

    static class B extends A implements I {
        int m() {
            return 2;
        }
    }

    static class C extends B {
        int m() {
            return 3;
        }
    }

    static int test1_invokevirtual(A a) {
        return a.m();
    }

    static int test1_1(A a) {
        return test1_invokevirtual(a);
    }

    static boolean test1() {
        A a = new A();
        B b = new B();
        C c = new C();

        // pollute profile at test1_invokevirtual to make sure the
        // compiler cannot rely on it
        for (int i = 0; i < 5000; i++) {
            test1_invokevirtual(a);
            test1_invokevirtual(b);
            test1_invokevirtual(c);
        }

        // profiling + speculation should make test1_invokevirtual
        // inline A.m() with a guard
        for (int i = 0; i < 20000; i++) {
            int res = test1_1(b);
            if (res != b.m()) {
                System.out.println("test1 failed with class B");
                return false;
            }
        }
        // check that the guard works as expected by passing a
        // different type
        int res = test1_1(a);
        if (res != a.m()) {
            System.out.println("test1 failed with class A");
            return false;
        }
        return true;
    }

    static int test2_invokevirtual(A a) {
        return a.m();
    }

    static int test2_1(A a, boolean t) {
        A aa;
        if (t) {
            aa = (B)a;
        } else {
            aa = a;
        }
        // if a of type B is passed to test2_1, the static type of aa
        // here is no better than A but the profiled type is B so this
        // should inline
        return test2_invokevirtual(aa);
    }

    static boolean test2() {
        A a = new A();
        B b = new B();
        C c = new C();

        // pollute profile at test2_invokevirtual to make sure the
        // compiler cannot rely on it
        for (int i = 0; i < 5000; i++) {
            test2_invokevirtual(a);
            test2_invokevirtual(b);
            test2_invokevirtual(c);
        }

        // profiling + speculation should make test2_invokevirtual
        // inline A.m() with a guard
        for (int i = 0; i < 20000; i++) {
            int res = test2_1(b, (i % 2) == 0);
            if (res != b.m()) {
                System.out.println("test2 failed with class B");
                return false;
            }
        }
        // check that the guard works as expected by passing a
        // different type
        int res = test2_1(a, false);
        if (res != a.m()) {
            System.out.println("test2 failed with class A");
            return false;
        }
        return true;
    }

    static int test3_invokevirtual(A a) {
        return a.m();
    }

    static void test3_2(A a) {
    }

    static int test3_1(A a, int i) {
        if (i == 0) {
            return 0;
        }
        // If we come here and a is of type B but parameter profiling
        // is polluted, both branches of the if below should have
        // profiling that tell us and inlining of the virtual call
        // should happen
        if (i == 1) {
            test3_2(a);
        } else {
            test3_2(a);
        }
        return test3_invokevirtual(a);
    }

    static boolean test3() {
        A a = new A();
        B b = new B();
        C c = new C();

        // pollute profile at test3_invokevirtual and test3_1 to make
        // sure the compiler cannot rely on it
        for (int i = 0; i < 3000; i++) {
            test3_invokevirtual(a);
            test3_invokevirtual(b);
            test3_invokevirtual(c);
            test3_1(a, 0);
            test3_1(b, 0);
        }

        // profiling + speculation should make test3_invokevirtual
        // inline A.m() with a guard
        for (int i = 0; i < 20000; i++) {
            int res = test3_1(b, (i % 2) + 1);
            if (res != b.m()) {
                System.out.println("test3 failed with class B");
                return false;
            }
        }
        // check that the guard works as expected by passing a
        // different type
        int res = test3_1(a, 1);
        if (res != a.m()) {
            System.out.println("test3 failed with class A");
            return false;
        }
        return true;
    }

    // Mix 2 incompatible profiled types
    static int test4_invokevirtual(A a) {
        return a.m();
    }

    static void test4_2(A a) {
    }

    static int test4_1(A a, boolean b) {
        if (b) {
            test4_2(a);
        } else {
            test4_2(a);
        }
        // shouldn't inline
        return test4_invokevirtual(a);
    }

    static boolean test4() {
        A a = new A();
        B b = new B();
        C c = new C();

        // pollute profile at test3_invokevirtual and test3_1 to make
        // sure the compiler cannot rely on it
        for (int i = 0; i < 3000; i++) {
            test4_invokevirtual(a);
            test4_invokevirtual(b);
            test4_invokevirtual(c);
        }

        for (int i = 0; i < 20000; i++) {
            if ((i % 2) == 0) {
                int res = test4_1(a, true);
                if (res != a.m()) {
                    System.out.println("test4 failed with class A");
                    return false;
                }
            } else {
                int res = test4_1(b, false);
                if (res != b.m()) {
                    System.out.println("test4 failed with class B");
                    return false;
                }
            }
        }
        return true;
    }

    // Mix one profiled type with an incompatible type
    static int test5_invokevirtual(A a) {
        return a.m();
    }

    static void test5_2(A a) {
    }

    static int test5_1(A a, boolean b) {
        if (b) {
            test5_2(a);
        } else {
            A aa = (B)a;
        }
        // shouldn't inline
        return test5_invokevirtual(a);
    }

    static boolean test5() {
        A a = new A();
        B b = new B();
        C c = new C();

        // pollute profile at test3_invokevirtual and test3_1 to make
        // sure the compiler cannot rely on it
        for (int i = 0; i < 3000; i++) {
            test5_invokevirtual(a);
            test5_invokevirtual(b);
            test5_invokevirtual(c);
        }

        for (int i = 0; i < 20000; i++) {
            if ((i % 2) == 0) {
                int res = test5_1(a, true);
                if (res != a.m()) {
                    System.out.println("test5 failed with class A");
                    return false;
                }
            } else {
                int res = test5_1(b, false);
                if (res != b.m()) {
                    System.out.println("test5 failed with class B");
                    return false;
                }
            }
        }
        return true;
    }

    // Mix incompatible profiled types
    static void test6_2(Object o) {
    }

    static Object test6_1(Object o, boolean b) {
        if (b) {
            test6_2(o);
        } else {
            test6_2(o);
        }
        return o;
    }

    static boolean test6() {
        A a = new A();
        A[] aa = new A[10];

        for (int i = 0; i < 20000; i++) {
            if ((i % 2) == 0) {
                test6_1(a, true);
            } else {
                test6_1(aa, false);
            }
        }
        return true;
    }

    // Mix a profiled type with an incompatible type
    static void test7_2(Object o) {
    }

    static Object test7_1(Object o, boolean b) {
        if (b) {
            test7_2(o);
        } else {
            Object oo = (A[])o;
        }
        return o;
    }

    static boolean test7() {
        A a = new A();
        A[] aa = new A[10];

        for (int i = 0; i < 20000; i++) {
            if ((i % 2) == 0) {
                test7_1(a, true);
            } else {
                test7_1(aa, false);
            }
        }
        return true;
    }

    // Mix a profiled type with an interface
    static void test8_2(Object o) {
    }

    static I test8_1(Object o) {
        test8_2(o);
        return (I)o;
    }

    static boolean test8() {
        A a = new A();
        B b = new B();
        C c = new C();

        for (int i = 0; i < 20000; i++) {
            test8_1(b);
        }
        return true;
    }

    // Mix a profiled type with a constant
    static void test9_2(Object o) {
    }

    static Object test9_1(Object o, boolean b) {
        Object oo;
        if (b) {
            test9_2(o);
            oo = o;
        } else {
            oo = "some string";
        }
        return oo;
    }

    static boolean test9() {
        A a = new A();

        for (int i = 0; i < 20000; i++) {
            if ((i % 2) == 0) {
                test9_1(a, true);
            } else {
                test9_1(a, false);
            }
        }
        return true;
    }

    // java/lang/Object:AnyNull:exact *,iid=top
    // meets
    // stable:bottom[int:max..0]:NotNull *
    static void test10_4(Object o) {
    }

    static void test10_3(Object o, boolean b) {
        if (b) {
            test10_4(o);
        }
    }

    static void test10_2(Object o, boolean b1, boolean b2) {
        if (b1) {
            test10_3(o, b2);
        }
    }

    static void test10_1(B[] b, boolean b1, boolean b2) {
        test10_2(b, b1, b2);
    }

    static boolean test10() {
        Object o = new Object();
        A[] a = new A[10];
        B[] b = new B[10];
        B[] c = new C[10];
        for (int i = 0; i < 20000; i++) {
            test10_1(b, false, false);
            test10_1(c, false, false);
            test10_2(a, true, false);
            test10_3(o, true);
        }
        return true;
    }

    // stable:TypeSpeculation$B:TopPTR *,iid=top[int:max..0]:TopPTR *,iid=top
    // meets
    // java/lang/Object:AnyNull:exact *,iid=top
    static void test11_3(Object o) {
    }

    static void test11_2(Object o, boolean b) {
        if (b) {
            test11_3(o);
        }
    }

    static void test11_1(B[] b, boolean bb) {
        test11_2(b, bb);
    }

    static boolean test11() {
        Object o = new Object();
        B[] b = new B[10];
        B[] c = new C[10];
        for (int i = 0; i < 20000; i++) {
            test11_1(b, false);
            test11_1(c, false);
            test11_2(o, true);
        }
        return true;
    }

    // TypeSpeculation$I *
    // meets
    // java/lang/Object:AnyNull *,iid=top
    static void test12_3(Object o) {
    }

    static void test12_2(Object o, boolean b) {
        if (b) {
            test12_3(o);
        }
    }

    static void test12_1(I i, boolean b) {
        test12_2(i, b);
    }

    static boolean test12() {
        Object o = new Object();
        B b = new B();
        C c = new C();
        for (int i = 0; i < 20000; i++) {
            test12_1(b, false);
            test12_1(c, false);
            test12_2(o, true);
        }
        return true;
    }

    // stable:bottom[int:max..0]:NotNull *
    // meets
    // stable:TypeSpeculation$A:TopPTR *,iid=top[int:max..0]:AnyNull:exact *,iid=top
    static Object test13_3(Object o, boolean b) {
        Object oo;
        if (b) {
            oo = o;
        } else {
            oo = new A[10];
        }
        return oo;
    }

    static void test13_2(Object o, boolean b1, boolean b2) {
        if (b1) {
            test13_3(o, b2);
        }
    }

    static void test13_1(B[] b, boolean b1, boolean b2) {
        test13_2(b, b1, b2);
    }

    static boolean test13() {
        A[] a = new A[10];
        B[] b = new B[10];
        B[] c = new C[10];
        for (int i = 0; i < 20000; i++) {
            test13_1(b, false, false);
            test13_1(c, false, false);
            test13_2(a, true, (i%2) == 0);
        }
        return true;
    }

    static public void main(String[] args) {
        boolean success = true;

        success = test1() && success;

        success = test2() && success;

        success = test3() && success;

        success = test4() && success;

        success = test5() && success;

        success = test6() && success;

        success = test7() && success;

        success = test8() && success;

        success = test9() && success;

        success = test10() && success;

        success = test11() && success;

        success = test12() && success;

        success = test13() && success;

        if (success) {
            System.out.println("TEST PASSED");
        } else {
            throw new RuntimeException("TEST FAILED: erroneous bound check elimination");
        }
    }
}
