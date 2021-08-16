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
 *   Test unbound method reference
 * @compile MethodRef3.java
 * @run main MethodRef3
 */

public class MethodRef3 {

    static interface A { String m(MethodRef3 mr); }

    static interface B { String m(MethodRef3 mr, String s); }

    private static void assertTrue(boolean cond) {
        if (!cond)
            throw new AssertionError();
    }

    String moo() {
        return "moo";
    }

    String wahoo(String s) {
        return "wahoo " + s;
    }

    public static void main(String[] args) {

        MethodRef3 mr = new MethodRef3();
        A a = MethodRef3::moo; //unbound reference to moo()
        assertTrue( a.m(mr).equals("moo") );
        B b = MethodRef3::wahoo; //unbound reference to wahoo()
        assertTrue( b.m(mr, "hi").equals("wahoo hi") );
    }
}
