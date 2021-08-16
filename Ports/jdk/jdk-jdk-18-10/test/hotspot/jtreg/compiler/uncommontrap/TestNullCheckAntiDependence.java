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

package compiler.uncommontrap;

/**
 * @test
 * @bug 8261730 8265132
 * @summary Test that no anti-dependence violation is reported between a store
 *          used as an implicit null check and a load placed in the null block.
 * @run main/othervm -XX:-BackgroundCompilation
 *      compiler.uncommontrap.TestNullCheckAntiDependence
 */

public class TestNullCheckAntiDependence {

    private static class MyInteger {
        int val;
    }

    private static MyInteger foo = new MyInteger();
    private static MyInteger bar = new MyInteger();
    private static MyInteger[] global = {new MyInteger()};

    static void test1() {
        for (int i = 0; i < 1; i++) {
            // This load is placed in the null block.
            foo.val = -bar.val;
            for (int k = 0; k < 10; k++) {
                // This store is hoisted and used as an implicit null check.
                foo.val = 0;
            }
        }
    }

    static void test2(MyInteger a, MyInteger b) {
        global[0].val = a.val + b.val * 31;
        global[0].val = 0;
        return;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10_000; i++) {
            test1();
        }

        for (int i = 0; i < 10_000; i++) {
            test2(new MyInteger(), new MyInteger());
        }

    }

}
