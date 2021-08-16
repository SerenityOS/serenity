/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6943289
 * @summary Project Coin: Improved Exception Handling for Java (aka 'multicatch')
 *
 */

public class Pos01 {

    static class A extends Exception {}
    static class B extends Exception {}

    static int caughtExceptions = 0;

    static void test(boolean b) {
        try {
            if (b) {
                throw new A();
            }
            else {
                throw new B();
            }
        }
        catch (final A | B ex) {
            caughtExceptions++;
        }
    }

    public static void main(String[] args) {
        test(true);
        test(false);
        if (caughtExceptions != 2) {
            throw new AssertionError("Exception handler called " + caughtExceptions + "times");
        }
    }
}
