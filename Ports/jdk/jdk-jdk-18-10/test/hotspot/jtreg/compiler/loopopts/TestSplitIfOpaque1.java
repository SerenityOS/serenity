/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8214994
 * @summary The split-if optimization fails because an unexpected Opaque1 is encountered.
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:-UseOnStackReplacement -XX:-BackgroundCompilation
 *                   compiler.loopopts.TestSplitIfOpaque1
 */

package compiler.loopopts;

public class TestSplitIfOpaque1 {

    static class MyClass {
        int f;
        MyClass(int f) {
            this.f = f;
        }
    }

    // The inner loop is found to be a counted loop and a loop limit check
    // is added by updating the Opaque1 input of the predicate template.
    // This Opaque1 node is then pushed upwards to before the predicates of
    // the outer loop and ends up right after the second 'if (b)'.
    // The split-if optimization kicks in and splits the first predicate of
    // the outer loop (an "obj != NULL" check) through the 'if (b)' region.
    // We fail because the region contains an unexpected Opaque1 node.
    static int test1(boolean b, int limit, MyClass obj) {
        int res = 0;
        MyClass notNull = new MyClass(42);
        if (b) {
            limit = 100;
        }
        if (b) {
            obj = notNull;
        }
        for (int i = 0; i < 1000; ++i) {
            res += obj.f;
            for (int j = 0; j <= limit; ++j) {
                // Empty
            }
        }
        return res;
    }

    // Same as test1 but triggers slightly different failure mode
    static int test2(boolean b, int limit, MyClass obj, int[] array) {
        int res = 0;
        MyClass notNull = new MyClass(12);
        if (b) {
            limit = 100;
        }
        if (b) {
            obj = notNull;
        }
        for (int i = 0; i < 1000; ++i) {
            res += obj.f;
            for (int j = 0; j <= limit; ++j) {
                array[j] = j;
            }
        }
        return res;
    }

    public static void main(String[] args) {
        MyClass obj = new MyClass(42);
        int[] array = new int[101];
        for (int i = 0; i < 20_000; i++) {
            test1(true, 50, obj);
            test1(false, 100, obj);
            test2(true, 50, obj, array);
            test2(false, 100, obj, array);
        }
    }
}
