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
 * @bug 8011706
 * @summary loop invariant code motion may move load before store to the same field
 *
 * @run main/othervm -XX:-UseOnStackReplacement -XX:-BackgroundCompilation
 *      compiler.c1.Test8011706
 */

package compiler.c1;

public class Test8011706 {
    int[] array;

    void m(boolean test, int[] array1, int[] array2) {
        int i = 0;
        if (test) {
            array = array1;
        } else {
            array = array2;
        }

        while(true) {
            int v = array[i];
            i++;
            if (i >= 10) return;
        }
    }

    static public void main(String[] args) {
        int[] new_array = new int[10];
        Test8011706 ti = new Test8011706();
        boolean failed = false;
        try {
            for (int i = 0; i < 10000; i++) {
                ti.array = null;
                ti.m(true, new_array, new_array);
            }
        } catch(NullPointerException ex) {
            throw new RuntimeException("TEST FAILED", ex);
        }
        System.out.println("TEST PASSED");
    }

}
