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
 * @bug 4855355
 * @summary implement "foreach" loops as specified in JSR 201
 * @author gafter
 */

import java.util.Iterator;

public class Foreach implements Iterable<Integer> {

    public Iterator<Integer> iterator() {
        return new Iterator<Integer>() {
            int next = 1;
            public boolean hasNext() {
                return next <= (1 << 12);
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
            public Integer next() {
                try {
                    return new Integer(next);
                } finally {
                    next <<= 1;
                }
            }
        };
    }

    public static void main(String[] args) {
        int sum;

        sum = 0;
        int[] a = new int[12];
        for (int i=0; i<12; i++) a[i] = 1<<i;
        for (int i : a) {
            if (i > 400) break;
            if (i == 16) continue;
            sum += i;
        }
        if (sum != 495)
            throw new AssertionError("cogito ergo " + sum);

        sum = 0;
        Iterable<Integer> x = new Foreach();
        for (Integer j : x)
            sum += j.intValue();
        if (sum != 8191)
            throw new AssertionError("cogito ergo " + sum);

        System.out.println("success!");
    }
}
