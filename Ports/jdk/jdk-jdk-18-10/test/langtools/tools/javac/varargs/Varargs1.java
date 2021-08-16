/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4856541 4812161
 * @summary varags, auto boxing
 * @author gafter
 *
 * @compile  Varargs1.java
 * @run main Varargs1
 */

public class Varargs1 {
    static int f(String s, int a, short b) {
        return 1;
    }
    static int f(String s, int a, int b) {
        return 2;
    }
    static int f(String s, Integer ... args) {
        return 3;
    }
    static int f(String s, Number ... args) {
        return 4;
    }
    static int f(String s, Object ... args) {
        return 5;
    }
    static int f(int ... args) {
        return 6;
    }
    static void check(int expected, int actual) {
        if (actual != expected) throw new AssertionError(actual);
    }
    public static void main(String[] args) {
        check(1, f("x", 12, (short)13));
        check(2, f("x", 12, 13));
        check(3, f("x", 12, 13, 14));
        check(4, f("x", 12, 13.5));
        check(5, f("x", 12, true));
        check(6, f(12, 13));
    }
}
