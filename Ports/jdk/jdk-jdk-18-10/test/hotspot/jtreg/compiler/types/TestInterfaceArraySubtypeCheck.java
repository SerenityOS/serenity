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
 *
 */

/*
 * @test
 * @bug 8266615
 * @summary C2 incorrectly folds subtype checks involving an interface array.
 * @run main/othervm -Xbatch
 *                   compiler.types.TestInterfaceArraySubtypeCheck
 */

package compiler.types;

public class TestInterfaceArraySubtypeCheck {

    static interface MyInterface { }

    static class MyClassA { }

    static class MyClassB extends MyClassA implements MyInterface { }

    static MyInterface[] getMyInterfaceArray() {
        return new MyClassB[0];
    }

    static MyInterface getMyInterface() {
        return new MyClassB();
    }

    static MyClassA[] test1() {
        return (MyClassA[])getMyInterfaceArray();
    }

    static void test2() {
        if (!(getMyInterfaceArray() instanceof MyClassA[])) {
            throw new RuntimeException("test2 failed");
        }
    }

    static MyClassA test3() {
        return (MyClassA)getMyInterface();
    }

    static void test4() {
        if (!(getMyInterface() instanceof MyClassA)) {
            throw new RuntimeException("test4 failed");
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 50_000; ++i) {
            test1();
            test2();
            test3();
            test4();
        }
    }
}
