/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8033626 8246453 8248467
 * @summary assert(ex_map->jvms()->same_calls_as(_exceptions->jvms())) failed: all collected exceptions must come from the same place
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 *
 * @run main/othervm -XX:-TieredCompilation -Xbatch
 *      -XX:CompileCommand=compileonly,compiler.intrinsics.object.TestClone::test*
 *      compiler.intrinsics.object.TestClone
 * @run main/othervm -XX:-TieredCompilation -Xbatch
 *      -XX:+IgnoreUnrecognizedVMOptions -XX:+StressReflectiveCode
 *      -XX:CompileCommand=compileonly,compiler.intrinsics.object.TestClone::test*
 *      compiler.intrinsics.object.TestClone
 * @run main/othervm -XX:-TieredCompilation -Xbatch
 *      -XX:+IgnoreUnrecognizedVMOptions -XX:+StressReflectiveCode -XX:+VerifyGraphEdges
 *      -XX:CompileCommand=compileonly,compiler.intrinsics.object.TestClone::test*
 *      compiler.intrinsics.object.TestClone
 */

package compiler.intrinsics.object;

import jdk.test.lib.Asserts;

abstract class MyAbstract {

    public Object myClone1() throws CloneNotSupportedException {
        return this.clone();
    }

    public Object myClone2() throws CloneNotSupportedException {
        return this.clone();
    }

    public Object myClone3() throws CloneNotSupportedException {
        return this.clone();
    }
}

class MyClass1 extends MyAbstract {
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}

class MyClass2 extends MyAbstract {

}

class MyClass3 extends MyAbstract implements Cloneable {
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}

class MyClass4 extends MyAbstract implements Cloneable {

}

public class TestClone implements Cloneable {
    static class A extends TestClone {}
    static class B extends TestClone {
        public B clone() {
            return (B)TestClone.b;
        }
    }
    static class C extends TestClone {
        public C clone() {
            return (C)TestClone.c;
        }
    }
    static class D extends TestClone {
        public D clone() {
            return (D)TestClone.d;
        }
    }
    static TestClone a = new A(), b = new B(), c = new C(), d = new D();

    public static Object test1(TestClone o) throws CloneNotSupportedException {
        // Polymorphic call site: >90% Object::clone / <10% other methods
        return o.clone();
    }

    public static void test2(MyAbstract obj, boolean shouldThrow) throws Exception {
       try {
            obj.myClone1();
        } catch (Exception e) {
            return; // Expected
        }
        Asserts.assertFalse(shouldThrow, "No exception thrown");
    }

    public static void test3(MyAbstract obj, boolean shouldThrow) throws Exception {
       try {
            obj.myClone2();
        } catch (Exception e) {
            return; // Expected
        }
        Asserts.assertFalse(shouldThrow, "No exception thrown");
    }

    public static void test4(MyAbstract obj, boolean shouldThrow) throws Exception {
       try {
            obj.myClone3();
        } catch (Exception e) {
            return; // Expected
        }
        Asserts.assertFalse(shouldThrow, "No exception thrown");
    }

    public static void main(String[] args) throws Exception {
        TestClone[] params1 = {a, a, a, a, a, a, a, a, a, a, a,
                               a, a, a, a, a, a, a, a, a, a, a,
                               a, a, a, a, a, a, a, a, a, a, a,
                               b, c, d};

        MyClass1 obj1 = new MyClass1();
        MyClass2 obj2 = new MyClass2();
        MyClass3 obj3 = new MyClass3();
        MyClass4 obj4 = new MyClass4();

        for (int i = 0; i < 15000; i++) {
            test1(params1[i % params1.length]);

            test2(obj1, true);
            test2(obj2, true);

            test3(obj3, false);
            test3(obj2, true);

            test4(obj3, false);
            test4(obj4, false);
        }

        Asserts.assertTrue(test1(a) != a);
        Asserts.assertTrue(test1(b) == b);
        Asserts.assertTrue(test1(c) == c);
        Asserts.assertTrue(test1(d) == d);

        try {
            test1(null);
            throw new AssertionError("No exception thrown");
        } catch (NullPointerException e) { /* expected */ }

        System.out.println("TEST PASSED");
    }
}
