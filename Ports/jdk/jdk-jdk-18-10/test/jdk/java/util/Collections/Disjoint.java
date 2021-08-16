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
 * @bug     4339792
 * @summary Basic test for Collections.disjoint
 * @author  Josh Bloch
 * @key randomness
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class Disjoint {
    static final int N = 20;

    public static void main(String[] args) {
        // Make an array of lists each of which shares a single element
        // with its "neighbors," and no elements with other lists in the array
        Random rnd = new Random();
        List[] lists = new List[N];
        int x = 0;
        for (int i = 0; i < N; i++) {
            int size = rnd.nextInt(10) + 2;
            List<Integer> list = new ArrayList<>(size);
            for (int j = 1; j < size; j++)
                list.add(x++);
            list.add(x);
            Collections.shuffle(list);

            lists[i] = list;
        }

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                boolean disjoint = (Math.abs(i - j) > 1);
                List<Integer> a = (List<Integer>) lists[i];
                List<Integer> b = (List<Integer>) lists[j];

                if (Collections.disjoint(a, b) != disjoint)
                    throw new RuntimeException("A: " + i + ", " + j);
                if (Collections.disjoint(new HashSet<Integer>(a), b)
                    != disjoint)
                    throw new RuntimeException("B: " + i + ", " + j);
                if (Collections.disjoint(new HashSet<Integer>(a),
                                         new HashSet<Integer>(b)) != disjoint)
                    throw new RuntimeException("C: " + i + ", " + j);
            }
        }
    }
}
