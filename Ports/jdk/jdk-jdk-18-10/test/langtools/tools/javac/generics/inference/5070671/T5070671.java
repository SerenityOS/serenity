/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     5070671
 * @summary Arrays.binarySearch can't infer int[]
 * @compile T5070671.java
 */

import java.util.*;

public class T5070671 {
    void foo1() {
        Comparator<int[]> c = new Comparator<int[]>() {
            public int compare(int[] c1, int[] c2) { return 0; }
        };
        int[][] arr = { { 1 } };
        int[] elem = arr[0];
        Arrays.sort(arr, c);
        Arrays.binarySearch(arr, elem, c);
        Arrays.<int[]>binarySearch(arr, elem, c);
    }
    void foo2() {
        Comparator<Integer[]> c = new Comparator<Integer[]>() {
            public int compare(Integer[] c1, Integer[] c2) { return 0; }
        };
        Integer[][] arr = { { 1 } };
        Integer[] elem = arr[0];
        Arrays.sort(arr, c);
        Arrays.binarySearch(arr, elem, c);
    }
}
