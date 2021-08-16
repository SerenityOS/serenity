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

package compiler.c2;

/**
 * @test
 * @bug 8266480
 * @summary Check correct re-wiring of control edge when hoisting a memory
 *          operation for an implicit null check.
 * @run main/othervm -Xbatch
 *                   compiler.c2.TestImplicitNullCheckDominance
 */
public class TestImplicitNullCheckDominance {

    double dFld;
    int iFld;

    static void test1(TestImplicitNullCheckDominance t, double d) {
        for (int i = 0; i < 100; i++) {
            t.dFld = d % 42;
            t.iFld = 43;
        }
    }

    static void test2(TestImplicitNullCheckDominance t) {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 100; j++) {
               t.dFld %= 42;
               t.iFld = 43;
            }
        }
    }

    public static void main(String[] args) {
        TestImplicitNullCheckDominance t = new TestImplicitNullCheckDominance();
        for (int i = 0; i < 50_000; ++i) {
            test1(t, i);
            test2(t);
        }
    }
}
