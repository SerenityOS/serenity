/*
 * Copyright (c) 2006, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     5073060
 * @summary Package private members not found for intersection types
 * @compile T5073060.java
 */

public class T5073060 {
    static String foo;
    public static void main(String[] args) {
        C2 c2 = null;
        C3 c3 = null;

        m1(c2, c3).c1m1();
        m1(c2, c3).i1m1();
        m1(c2, c3).i2m1();
    }

    public static <T> T m1(T t1, T t2) {
        return null;
    }

    class C1 { void c1m1() {} }
    interface I1 { void i1m1(); }
    interface I2 { void i2m1(); }

    class C2 extends C1 implements I1, I2 {
        public void i1m1() { }
        public void i2m1() { }
    }

    class C3 extends C1 implements I1, I2 {
        public void i1m1() { }
        public void i2m1() { }
    }
}
