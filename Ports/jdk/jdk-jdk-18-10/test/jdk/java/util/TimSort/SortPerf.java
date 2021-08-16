/*
 * Copyright 2009 Google Inc.  All Rights Reserved.
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

public class SortPerf {
    private static final int NUM_SETS = 5;
    private static final int[] lengths = { 10, 100, 1000, 10000, 1000000 };

    // Returns the number of repetitions as a function of the list length
    private static int reps(int n) {
        return (int) (12000000 / (n * Math.log10(n)));
    }

    public static void main(String[] args) {
        Sorter.warmup();

        System.out.print("Strategy,Length");
        for (Sorter sorter : Sorter.values())
            System.out.print("," + sorter);
        System.out.println();

        for (ArrayBuilder ab : ArrayBuilder.values()) {
            for (int n : lengths) {
                System.out.printf("%s,%d", ab, n);
                int reps = reps(n);
                Object[] proto = ab.build(n);
                for (Sorter sorter : Sorter.values()) {
                    double minTime = Double.POSITIVE_INFINITY;
                    for (int set = 0; set < NUM_SETS; set++) {
                        long startTime = System.nanoTime();
                        for (int k = 0; k < reps; k++) {
                            Object[] a = proto.clone();
                            sorter.sort(a);
                        }
                        long endTime = System.nanoTime();
                        double time = (endTime - startTime) / (1000000. * reps);
                        minTime = Math.min(minTime, time);
                    }
                    System.out.printf(",%5f", minTime);
                }
                System.out.println();
            }
        }
    }
}
