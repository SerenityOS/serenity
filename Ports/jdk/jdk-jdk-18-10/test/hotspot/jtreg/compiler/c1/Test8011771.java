/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8011771
 * @summary Array bound check elimination's in block motion doesn't always reset its data structures from one step to the other.
 *
 * @run main/othervm -XX:-BackgroundCompilation compiler.c1.Test8011771
 */

package compiler.c1;

public class Test8011771 {

    static void m(int[] a, int[] b, int j) {
        // Array bound check elimination inserts a predicate before
        // the loop. We'll have the predicate fail, so the method is
        // recompiled without optimistic optimizations
        for (int i = 0; i < 10; i++) {
            a[i] = i;
        }

        // The test itself
        a[j] = 0;
        a[j+5] = 0;
        b[j+4] = 0; // this range check shouldn't be eliminated
    }

    static public void main(String[] args) {
        int[] arr1 = new int[10], arr2 = new int[10];
        // force compilation:
        for (int i = 0; i < 5000; i++) {
            m(arr1, arr2, 0);
        }

        try {
            m(new int[1], null, 0); // force predicate failure
        } catch(ArrayIndexOutOfBoundsException e) {}

        // force compilation again (no optimistic opts):
        for (int i = 0; i < 5000; i++) {
            m(arr1, arr2, 0);
        }

        // Check that the range check  on the second array wasn't optimized out
        boolean success = false;
        try {
            m(arr1, new int[1], 0);
        } catch(ArrayIndexOutOfBoundsException e) {
            success = true;
        }
        if (success) {
            System.out.println("TEST PASSED");
        } else {
            throw new RuntimeException("TEST FAILED: erroneous bound check elimination");
        }
    }
}
