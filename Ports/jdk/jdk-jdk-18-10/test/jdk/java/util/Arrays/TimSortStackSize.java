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
 * @bug 8011944
 * @summary Test TimSort stack size
 */
import java.util.Arrays;
import java.util.ArrayDeque;

public class TimSortStackSize {

    public static void main(String[] args) {
        testComparableTimSort();
        testTimSort();
    }

    static void testComparableTimSort() {
        System.out.printf("testComparableTimSort()%n");
        Arrays.sort(genData());
    }

    static void testTimSort() {
        System.out.printf("testTimSort()%n");
        Arrays.sort(genData(), Integer::compare);
    }

    private static final int MIN = 16;

    private static final int BOUND1 = 2 * MIN + 1;
    private static final int BOUND2 = BOUND1 + MIN + 2;
    private static final int BOUND3 = BOUND1 + 1 + BOUND2;
    private static final int BOUND4 = BOUND2 + 1 + BOUND3;
    private static final int BOUND5 = BOUND3 + 1 + BOUND4;

    static int build(int size, int B, ArrayDeque<Integer> chunks) {
        chunks.addFirst(B);
        if (size < BOUND1) {
            chunks.addFirst(size);
            return size;
        }

        int asize = (size + 2) / 2;
        if (size >= BOUND2 && asize < BOUND1) {
            asize = BOUND1;
        } else if (size >= BOUND3 && asize < BOUND2) {
            asize = BOUND2;
        } else if (size >= BOUND4 && asize < BOUND3) {
            asize = BOUND3;
        } else if (size >= BOUND5 && asize < BOUND4) {
            asize = BOUND4;
        }
        if (size - asize >= B) {
            throw new AssertionError(" " + size + " , " + asize + " , " + B);
        }
        return build(asize, size - asize, chunks);
    }

    static Integer[] genData() {
        ArrayDeque<Integer> chunks = new ArrayDeque<Integer>();
        chunks.addFirst(MIN);

        int B = MIN + 4;
        int A = B + MIN + 1;

        for (int i = 0; i < 8; i++) {
            int eps = build(A, B, chunks);
            B = B + A + 1;
            A = B + eps + 1;
        }
        chunks.addFirst(B);
        chunks.addFirst(A);
        int total = 0;
        for (Integer len : chunks) {
            total += len;
        }
        int pow = MIN;
        while (pow < total) {
            pow += pow;
        }
        chunks.addLast(pow - total);
        System.out.println(" Total: " + total);
        Integer[] array = new Integer[pow];
        int off = 0;
        int pos = 0;
        for (Integer len : chunks) {
            for (int i = 0; i < len; i++) {
                array[pos++] = Integer.valueOf(i == 0 ? 0 : 1);
            }
            off++;
        }
        return array;
    }

}
