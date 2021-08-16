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
 * @bug 8003280
 * @summary Add lambda tests
 *   Test instance method reference
 * @compile MethodRef2.java
 * @run main MethodRef2
 */

public class MethodRef2 {

    static interface A {String m();}

    static interface B {String m(int i);}

    private static void assertTrue(boolean cond) {
        if (!cond)
            throw new AssertionError();
    }

    String moo() {
        return "moo";
    }

    String wahoo() {
        return "wahoo";
    }

    String wahoo(int x) {
        return "wahoo " + x;
    }

    public static void main(String[] args) {

        MethodRef2 mr = new MethodRef2();

        A a = mr::moo; //instance reference to moo()
        assertTrue( a.m().equals("moo") );

        a = new MethodRef2()::wahoo; //instance reference to wahoo()
        assertTrue( a.m().equals("wahoo") );

        B b = mr::wahoo; //instance reference to wahoo(int)
        assertTrue( b.m(4).equals("wahoo 4") );
    }
}
