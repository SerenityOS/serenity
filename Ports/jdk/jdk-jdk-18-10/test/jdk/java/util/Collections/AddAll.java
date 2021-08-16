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
 * @bug     4822887
 * @summary Basic test for Collections.addAll
 * @author  Josh Bloch
 * @key randomness
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class AddAll {
    static final int N = 100;
    public static void main(String[] args) {
        test(new ArrayList<Integer>());
        test(new LinkedList<Integer>());
        test(new HashSet<Integer>());
        test(new LinkedHashSet<Integer>());
    }

    private static Random rnd = new Random();

    static void test(Collection<Integer> c) {
        int x = 0;
        for (int i = 0; i < N; i++) {
            int rangeLen = rnd.nextInt(10);
            if (Collections.addAll(c, range(x, x + rangeLen)) !=
                (rangeLen != 0))
                throw new RuntimeException("" + rangeLen);
            x += rangeLen;
        }
        if (c instanceof List) {
            if (!c.equals(Arrays.asList(range(0, x))))
                throw new RuntimeException(x +": "+c);
        } else {
            if (!c.equals(new HashSet<Integer>(Arrays.asList(range(0, x)))))
                throw new RuntimeException(x +": "+c);
        }
    }

    private static Integer[] range(int from, int to) {
        Integer[] result = new Integer[to - from];
        for (int i = from, j=0; i < to; i++, j++)
            result[j] = new Integer(i);
        return result;
    }
}
